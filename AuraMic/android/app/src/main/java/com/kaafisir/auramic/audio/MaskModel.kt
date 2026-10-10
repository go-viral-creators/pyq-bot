package com.kaafisir.auramic.audio

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TFLite (LiteRT) band-mask refinement model.
 *
 * Model contract (`assets/aura_mask.tflite`, built by tools/train_mask_model.py):
 *   input  : float32 [N, 320]  = 5 consecutive frames (t-2..t+2) x 64 DSP features
 *   output : float32 [N, 32]   = per-band speech-keep mask in (0,1)
 * The native engine blends this mask with its own Wiener/LSA gain. If the asset is missing or
 * fails to load, [predict] returns null and the pure-DSP path is used - audio still gets cleaned.
 */
@Singleton
class MaskModel @Inject constructor(@ApplicationContext private val context: Context) {

    private val lock = Any()
    private var interpreter: Interpreter? = null
    private var loaded = false
    private var lastBatch = -1

    val isAvailable: Boolean get() = synchronized(lock) { ensureLoaded() != null }

    private fun ensureLoaded(): Interpreter? {
        if (loaded) return interpreter
        loaded = true
        interpreter = runCatching {
            val fd = context.assets.openFd(ASSET)
            val buffer: MappedByteBuffer = fd.createInputStream().channel.use { ch ->
                ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
            Interpreter(buffer, Interpreter.Options().apply { setNumThreads(2) }).also { itp ->
                val shape = itp.getInputTensor(0).shape()
                require(shape.size == 2 && shape[1] == IN_DIM) { "unexpected input shape ${shape.toList()}" }
            }
        }.onFailure { Log.w(TAG, "Mask model unavailable, using DSP only: ${it.message}") }.getOrNull()
        return interpreter
    }

    /**
     * @param features frames * [NativeDenoiser.FEATURES] values from [NativeDenoiser.extractFeatures]
     * @return frames * [NativeDenoiser.BANDS] mask values, or null if the model can't run.
     */
    fun predict(features: FloatArray, frames: Int): FloatArray? = synchronized(lock) {
        val itp = ensureLoaded() ?: return null
        if (frames <= 0) return FloatArray(0)
        val f = NativeDenoiser.FEATURES
        val bands = NativeDenoiser.BANDS
        val result = FloatArray(frames * bands)
        return try {
            var start = 0
            while (start < frames) {
                val n = minOf(BATCH, frames - start)
                val input = ByteBuffer.allocateDirect(n * IN_DIM * 4).order(ByteOrder.nativeOrder())
                val inF = input.asFloatBuffer()
                for (i in 0 until n) {
                    val t = start + i
                    for (c in -CONTEXT..CONTEXT) {
                        val src = (t + c).coerceIn(0, frames - 1) * f
                        inF.put(features, src, f)
                    }
                }
                if (n != lastBatch) {
                    itp.resizeInput(0, intArrayOf(n, IN_DIM))
                    itp.allocateTensors()
                    lastBatch = n
                }
                val output = ByteBuffer.allocateDirect(n * bands * 4).order(ByteOrder.nativeOrder())
                itp.run(input, output)
                output.rewind()
                output.asFloatBuffer().get(result, start * bands, n * bands)
                start += n
            }
            for (i in result.indices) result[i] = result[i].coerceIn(0.001f, 1f)
            result
        } catch (t: Throwable) {
            Log.w(TAG, "Mask inference failed, falling back to DSP only", t)
            null
        }
    }

    companion object {
        private const val TAG = "MaskModel"
        const val ASSET = "aura_mask.tflite"
        const val CONTEXT = 2
        const val IN_DIM = (2 * CONTEXT + 1) * NativeDenoiser.FEATURES
        private const val BATCH = 1024
    }
}
