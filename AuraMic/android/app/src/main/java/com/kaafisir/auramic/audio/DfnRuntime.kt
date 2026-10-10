package com.kaafisir.auramic.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * DeepFilterNet3 (full-band 48 kHz speech enhancement, ~2.1 M parameters) on ONNX Runtime - platform independent.
 *
 * Whole-clip processing with bounded memory:
 *   native: STFT + features  ->  ORT: encoder, ERB decoder, deep-filter decoder (in chunks)  ->  native: gains / DF / iSTFT.
 * The network is causal with conv_lookahead = 2, so the outputs for frame o are the network outputs at index o+2
 * (features zero-padded past the end) - verified against the libDF reference (tools/test_dfn.py).
 * Each chunk is run with [CONTEXT] frames of left context so conv/GRU history is warmed up.
 *
 * @param loadModel returns the bytes of "enc", "erb_dec" or "df_dec" (ORT-format .ort models)
 */
class DfnRuntime(
    private val threads: Int,
    private val loadModel: (String) -> ByteArray,
) : AiEnhancer {

    private class Sessions(val env: OrtEnvironment, val enc: OrtSession, val erbDec: OrtSession, val dfDec: OrtSession)

    private val lock = Any()
    private var sessions: Sessions? = null
    private var failed = false

    override val isAvailable: Boolean get() = ensure() != null

    /** Loads the models now (e.g. from a background thread) so the first clip does not pay the start-up cost. */
    fun warmUp() { ensure() }

    private fun ensure(): Sessions? = synchronized(lock) {
        if (sessions != null || failed) return sessions
        try {
            val t0 = System.nanoTime()
            val env = OrtEnvironment.getEnvironment()
            fun load(name: String): OrtSession {
                val opts = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(max(1, threads))
                    setInterOpNumThreads(1)
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT) // .ort models are already optimized (and the minimal runtime has no optimizer)
                    setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
                    addConfigEntry("session.intra_op.allow_spinning", "0") // do not burn battery between calls
                }
                return env.createSession(loadModel(name), opts)
            }
            sessions = Sessions(env, load("enc"), load("erb_dec"), load("df_dec"))
            AudioLog.i(TAG, "DeepFilterNet3 loaded in ${(System.nanoTime() - t0) / 1_000_000} ms")
        } catch (t: Throwable) {
            AudioLog.w(TAG, "DeepFilterNet unavailable, falling back to the classic engine", t)
            failed = true
        }
        sessions
    }

    /**
     * @param x 48 kHz mono float samples (length >= [n])
     * @param attenDb maximum attenuation, >= 100 for unlimited
     * @param fast skip the deep-filtering decoder (mask only): ~30 % faster, a little less detail
     * @return enhanced samples (length n, aligned with the input) or null if the model could not run
     */
    override fun enhance(x: FloatArray, n: Int, attenDb: Float, fast: Boolean, onProgress: ((Float) -> Unit)?): FloatArray? {
        val s = ensure() ?: return null
        if (n <= 0) return FloatArray(0)
        return try {
            NativeDfn().use { dfn ->
                val total = dfn.load(x, n)
                var start = 0
                while (start < total) {
                    val end = min(total, start + CHUNK)
                    runChunk(s, dfn, start, end, attenDb, !fast)
                    start = end
                    onProgress?.invoke(start / total.toFloat())
                }
                FloatArray(n).also { dfn.output(it) }
            }
        } catch (t: Throwable) {
            AudioLog.w(TAG, "DeepFilterNet inference failed", t)
            null
        }
    }

    private fun runChunk(s: Sessions, dfn: NativeDfn, start: Int, end: Int, attenDb: Float, useDf: Boolean) {
        val fs = max(0, start - CONTEXT)       // first feature frame fed to the network
        val fe = end + LOOKAHEAD               // + conv lookahead (zero padded past the clip end by the native side)
        val cnt = fe - fs
        val erb = FloatArray(cnt * NativeDfn.ERB)
        val spec = FloatArray(2 * cnt * NativeDfn.DF_BINS)
        dfn.fillFeatures(fs, cnt, erb, spec)

        val tErb = tensor(s.env, erb, longArrayOf(1, 1, cnt.toLong(), NativeDfn.ERB.toLong()))
        val tSpec = tensor(s.env, spec, longArrayOf(1, 2, cnt.toLong(), NativeDfn.DF_BINS.toLong()))
        try {
            s.enc.run(mapOf("feat_erb" to tErb, "feat_spec" to tSpec)).use { enc ->
                fun t(name: String) = enc.get(name).get() as OnnxTensor
                val n = end - start
                val a = (start + LOOKAHEAD) - fs   // index of aligned frame `start` in the network output

                val lsnrAll = FloatArray(cnt).also { t("lsnr").floatBuffer.get(it) }
                val lsnr = lsnrAll.copyOfRange(a, a + n)

                val mask = s.erbDec.run(
                    mapOf("emb" to t("emb"), "e3" to t("e3"), "e2" to t("e2"), "e1" to t("e1"), "e0" to t("e0")),
                ).use { r ->
                    val buf = (r.get("m").get() as OnnxTensor).floatBuffer
                    FloatArray(cnt * NativeDfn.ERB).also { buf.get(it) }.copyOfRange(a * NativeDfn.ERB, (a + n) * NativeDfn.ERB)
                }
                val per = NativeDfn.DF_BINS * NativeDfn.DF_ORDER * 2
                val coefs = if (useDf) {
                    s.dfDec.run(mapOf("emb" to t("emb"), "c0" to t("c0"))).use { r ->
                        val buf = (r.get("coefs").get() as OnnxTensor).floatBuffer
                        FloatArray(cnt * per).also { buf.get(it) }.copyOfRange(a * per, (a + n) * per)
                    }
                } else null
                dfn.apply(start, n, lsnr, mask, coefs, attenDb, useDf)
            }
        } finally {
            tErb.close(); tSpec.close()
        }
    }

    private fun tensor(env: OrtEnvironment, data: FloatArray, shape: LongArray): OnnxTensor {
        val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
        val fb: FloatBuffer = bb.asFloatBuffer().also { it.put(data); it.rewind() }
        return OnnxTensor.createTensor(env, fb, shape)
    }

    companion object {
        private const val TAG = "DfnRuntime"
        const val CHUNK = 800     // frames (8 s) per network call; keep in sync with tools/dfn_native.py
        const val CONTEXT = 200   // frames of left context (conv + GRU warm-up)
        const val LOOKAHEAD = 2   // conv_lookahead
    }
}
