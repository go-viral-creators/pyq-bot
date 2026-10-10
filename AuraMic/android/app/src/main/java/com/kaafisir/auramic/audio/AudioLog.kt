package com.kaafisir.auramic.audio

/** Tiny logging hook so the audio code stays free of android.* imports (it is also unit-tested on a plain JVM). */
object AudioLog {
    @JvmStatic var sink: (String, String) -> Unit = { tag, msg -> System.err.println("[$tag] $msg") }
    fun i(tag: String, msg: String) = sink(tag, msg)
    fun w(tag: String, msg: String, t: Throwable? = null) = sink(tag, msg + (t?.let { " :: $it" } ?: ""))
}

/** Something that can run the neural denoiser; the Android implementation is [DfnEnhancer]. */
interface AiEnhancer {
    val isAvailable: Boolean
    fun enhance(x: FloatArray, n: Int, attenDb: Float, fast: Boolean, onProgress: ((Float) -> Unit)? = null): FloatArray?
}
