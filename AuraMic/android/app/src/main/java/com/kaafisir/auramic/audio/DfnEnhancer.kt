package com.kaafisir.auramic.audio

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/** Android wrapper: loads the DeepFilterNet3 ONNX models from assets and hands them to [DfnRuntime]. */
@Singleton
class DfnEnhancer @Inject constructor(@ApplicationContext private val context: Context) : AiEnhancer {

    private val runtime = DfnRuntime(
        threads = min(4, max(1, Runtime.getRuntime().availableProcessors())),
        loadModel = { name -> context.assets.open("dfn3/$name.ort").use { it.readBytes() } },
    )

    init { AudioLog.sink = { tag, msg -> Log.i(tag, msg) } }

    override val isAvailable: Boolean get() = runtime.isAvailable

    /** Loads the models off the main thread so the first voice note does not pay the start-up cost. */
    fun warmUp() { Thread({ runtime.warmUp() }, "auramic-dfn-warmup").start() }

    override fun enhance(x: FloatArray, n: Int, attenDb: Float, fast: Boolean, onProgress: ((Float) -> Unit)?): FloatArray? =
        runtime.enhance(x, n, attenDb, fast, onProgress)
}
