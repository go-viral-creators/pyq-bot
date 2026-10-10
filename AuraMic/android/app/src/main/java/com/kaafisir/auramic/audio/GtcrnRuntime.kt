package com.kaafisir.auramic.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GTCRN (48 k parameters, ~33 MMAC/s) on ONNX Runtime: a very small, very fast speech cleaner that works at 16 kHz.
 * Pipeline: 48 kHz -> 16 kHz -> STFT(512/256, sqrt-Hann) -> network -> iSTFT -> 48 kHz.
 * The network is stateless over the whole clip (the export takes the complete spectrogram), so there is no chunking.
 *
 * @param loadModel returns the bytes of gtcrn_dns3.ort
 */
class GtcrnRuntime(
    private val threads: Int,
    private val loadModel: () -> ByteArray,
) : LightEnhancer {

    private val lock = Any()
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var failed = false

    override val isAvailable: Boolean get() = ensure() != null

    fun warmUp() { ensure() }

    private fun ensure(): OrtSession? = synchronized(lock) {
        if (session != null || failed) return session
        try {
            val e = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(max(1, threads))
                setInterOpNumThreads(1)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT)
                addConfigEntry("session.intra_op.allow_spinning", "0")
            }
            session = e.createSession(loadModel(), opts)
            env = e
        } catch (t: Throwable) {
            AudioLog.w(TAG, "light cleaner unavailable, falling back to the classic one", t)
            failed = true
        }
        session
    }

    override fun enhance(x: FloatArray, n: Int, onProgress: ((Float) -> Unit)?): FloatArray? {
        val s = ensure() ?: return null
        if (n <= 0) return FloatArray(0)
        return try {
            val x16 = Resampler.resample(x, n, 48_000, SR)
            if (x16.size < FFT) return x.copyOf(n)
            onProgress?.invoke(0.1f)
            val spec = stft(x16)                                   // [257][T][2] flattened
            val frames = spec.size / (BINS * 2)
            val shape = longArrayOf(1, BINS.toLong(), frames.toLong(), 2)
            val bb = ByteBuffer.allocateDirect(spec.size * 4).order(ByteOrder.nativeOrder())
            bb.asFloatBuffer().put(spec)
            val input = OnnxTensor.createTensor(env, bb.asFloatBuffer(), shape)
            val outSpec = FloatArray(spec.size)
            try {
                s.run(mapOf("spec" to input)).use { r -> (r.get(0) as OnnxTensor).floatBuffer.get(outSpec) }
            } finally { input.close() }
            onProgress?.invoke(0.7f)
            val y16 = istft(outSpec, frames, x16.size)
            val y = Resampler.resample(y16, y16.size, SR, 48_000)
            FloatArray(n) { if (it < y.size) y[it] else 0f }
        } catch (t: Throwable) {
            AudioLog.w(TAG, "light cleaner failed", t)
            null
        }
    }

    // ---- STFT / iSTFT identical to torch.stft/istft(n_fft=512, hop=256, win=sqrt(hann), center=True) ---------------------

    private val win = FloatArray(FFT) { sqrt(0.5 - 0.5 * cos(2 * PI * it / FFT)).toFloat() }
    private val cosT = FloatArray(FFT / 2) { cos(2 * PI * it / FFT).toFloat() }
    private val sinT = FloatArray(FFT / 2) { -sin(2 * PI * it / FFT).toFloat() }
    private val rev = IntArray(FFT).also { r -> val bits = 9; for (i in 0 until FFT) { var v = i; var o = 0; for (b in 0 until bits) { o = (o shl 1) or (v and 1); v = v shr 1 }; r[i] = o } }

    /** In-place radix-2 complex FFT (forward when [inverse] is false; inverse is unscaled). */
    private fun fft(re: FloatArray, im: FloatArray, inverse: Boolean) {
        for (i in 0 until FFT) { val j = rev[i]; if (j > i) { val a = re[i]; re[i] = re[j]; re[j] = a; val b = im[i]; im[i] = im[j]; im[j] = b } }
        var len = 2
        while (len <= FFT) {
            val half = len / 2; val step = FFT / len
            var i = 0
            while (i < FFT) {
                for (k in 0 until half) {
                    val wr = cosT[k * step]; val wi = if (inverse) -sinT[k * step] else sinT[k * step]
                    val a = i + k; val b = a + half
                    val tr = re[b] * wr - im[b] * wi; val ti = re[b] * wi + im[b] * wr
                    re[b] = re[a] - tr; im[b] = im[a] - ti; re[a] += tr; im[a] += ti
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun reflect(i: Int, n: Int): Int { var k = i; if (k < 0) k = -k; if (k >= n) k = 2 * (n - 1) - k; return k }

    private fun stft(x: FloatArray): FloatArray {
        val n = x.size
        val frames = 1 + n / HOP
        val out = FloatArray(BINS * frames * 2)
        val re = FloatArray(FFT); val im = FloatArray(FFT)
        for (t in 0 until frames) {
            for (i in 0 until FFT) { re[i] = x[reflect(t * HOP + i - FFT / 2, n)] * win[i]; im[i] = 0f }
            fft(re, im, false)
            for (f in 0 until BINS) { val o = (f * frames + t) * 2; out[o] = re[f]; out[o + 1] = im[f] }
        }
        return out
    }

    private fun istft(spec: FloatArray, frames: Int, length: Int): FloatArray {
        val total = (frames - 1) * HOP + FFT
        val acc = FloatArray(total); val env = FloatArray(total)
        val re = FloatArray(FFT); val im = FloatArray(FFT)
        for (t in 0 until frames) {
            for (f in 0 until BINS) { val o = (f * frames + t) * 2; re[f] = spec[o]; im[f] = spec[o + 1] }
            for (f in 1 until FFT / 2) { re[FFT - f] = re[f]; im[FFT - f] = -im[f] }
            im[0] = 0f; im[FFT / 2] = 0f
            fft(re, im, true)
            val base = t * HOP
            for (i in 0 until FFT) { acc[base + i] += re[i] / FFT * win[i]; env[base + i] += win[i] * win[i] }
        }
        return FloatArray(length) { i -> val j = i + FFT / 2; if (j < total && env[j] > 1e-8f) acc[j] / env[j] else 0f }
    }

    companion object {
        private const val TAG = "GtcrnRuntime"
        const val SR = 16_000
        private const val FFT = 512
        private const val HOP = 256
        private const val BINS = FFT / 2 + 1
    }
}
