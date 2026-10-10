package com.kaafisir.auramic.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/** Android wrapper: loads the GTCRN ONNX model from assets and hands it to [GtcrnRuntime]. */
@Singleton
class GtcrnEnhancer @Inject constructor(@ApplicationContext private val context: Context) : LightEnhancer {
    private val runtime = GtcrnRuntime(
        threads = min(2, max(1, Runtime.getRuntime().availableProcessors())),
        loadModel = { context.assets.open("gtcrn/gtcrn_dns3.ort").use { it.readBytes() } },
    )
    override val isAvailable: Boolean get() = runtime.isAvailable
    fun warmUp() { Thread({ runtime.warmUp() }, "auramic-gtcrn-warmup").start() }
    override fun enhance(x: FloatArray, n: Int, onProgress: ((Float) -> Unit)?): FloatArray? = runtime.enhance(x, n, onProgress)
}
