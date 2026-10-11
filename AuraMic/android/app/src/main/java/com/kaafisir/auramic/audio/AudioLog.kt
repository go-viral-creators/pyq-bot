package com.kaafisir.auramic.audio

/** Tiny logging hook so the audio code stays free of android.* imports (it is also unit-tested on a plain JVM). */
object AudioLog {
    @JvmStatic var sink: (String, String) -> Unit = { tag, msg -> System.err.println("[$tag] $msg") }
    fun i(tag: String, msg: String) = sink(tag, msg)
    /** The most recent warning and the engine of the last processed note, for "Copy support info". */
    @Volatile @JvmStatic var lastIssue: String? = null
    @Volatile @JvmStatic var lastEngine: String? = null
    fun w(tag: String, msg: String, t: Throwable? = null) {
        val line = msg + (t?.let { " :: $it" } ?: "")
        lastIssue = line.take(300)
        sink(tag, line)
    }
}

/** Something that can run the neural denoiser; the Android implementation is [DfnEnhancer]. */
interface AiEnhancer {
    val isAvailable: Boolean
    fun enhance(x: FloatArray, n: Int, attenDb: Float, fast: Boolean, onProgress: ((Float) -> Unit)? = null): FloatArray?
}

/** The light, fast speech cleaner used for Medium Quality (GTCRN on ONNX Runtime). Works on 48 kHz audio. */
interface LightEnhancer {
    val isAvailable: Boolean
    fun enhance(x: FloatArray, n: Int, onProgress: ((Float) -> Unit)? = null): FloatArray?
}
