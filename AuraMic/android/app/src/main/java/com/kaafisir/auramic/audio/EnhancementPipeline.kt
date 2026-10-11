package com.kaafisir.auramic.audio

import com.kaafisir.auramic.domain.model.EnhanceEngine
import com.kaafisir.auramic.domain.model.EnhanceOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/** Output of [EnhancementPipeline]. All signals are 48 kHz mono float. */
class EnhanceResult(
    /** Final, mastered voice (this is what gets encoded and sent). */
    val voice: FloatArray,
    val length: Int,
    /** Only filled when stems were requested (Studio Lab): level-matched original and the removed background. */
    val original: FloatArray?,
    val removed: FloatArray?,
    val engineUsed: EnhanceEngine,
    val processingMs: Long,
    val inputSeconds: Float,
    val removedSilenceSeconds: Float,
    val report: NativePolish.Report?,
) {
    val sampleRate: Int get() = SAMPLE_RATE
    val seconds: Float get() = length / SAMPLE_RATE.toFloat()
    /** How many times faster than real time the clip was processed. */
    val speedFactor: Float get() = if (processingMs <= 0) 0f else inputSeconds * 1000f / processingMs

    companion object { const val SAMPLE_RATE = 48_000 }
}

/**
 * The full voice-note pipeline:
 *   resample to 48 kHz -> denoise (DeepFilterNet3 for High, GTCRN for Medium, classic DSP as a fallback) -> keep-ambience mix
 *   -> auto-cut silence -> mastering chain (EQ / de-ess / compressor / LUFS / limiter)
 */
@Singleton
class EnhancementPipeline @Inject constructor(private val dfn: AiEnhancer, private val light: LightEnhancer) {

    fun process(
        pcm: ShortArray,
        length: Int,
        sampleRate: Int,
        opt: EnhanceOptions,
        wantStems: Boolean = false,
        onProgress: ((Float) -> Unit)? = null,
    ): EnhanceResult {
        val t0 = System.nanoTime()
        val raw = Resampler.toFloat(pcm, length)
        val x = if (sampleRate == EnhanceResult.SAMPLE_RATE) raw else Resampler.resample(raw, length, sampleRate, EnhanceResult.SAMPLE_RATE)
        val n = x.size
        val inputSeconds = n / EnhanceResult.SAMPLE_RATE.toFloat()

        var engineUsed = opt.engine
        var enhanced: FloatArray? = null
        if (opt.engine == EnhanceEngine.AI) {
            enhanced = dfn.enhance(x, n, opt.attenuationDb, opt.fastMode, onProgress)
            if (enhanced == null) engineUsed = EnhanceEngine.CLASSIC
        }
        if (opt.engine == EnhanceEngine.LIGHT) {
            enhanced = light.enhance(x, n, onProgress)
            if (enhanced == null) engineUsed = EnhanceEngine.CLASSIC
        }
        if (enhanced == null) enhanced = classic(x, n, opt)
        onProgress?.invoke(0.9f)

        val removed = if (wantStems) FloatArray(n) { x[it] - enhanced[it] } else null

        // "Keep ambience": bring back a share of what was removed (0 = studio silent, 1 = original)
        val keep = opt.keepBackground
        var voice = enhanced
        if (keep > 0.001f) for (i in 0 until n) voice[i] += keep * (x[i] - enhanced[i])

        var len = n
        var removedSilence = 0
        if (opt.silenceCut != com.kaafisir.auramic.domain.model.SilenceCut.OFF) {
            val r = SilenceCutter.cut(voice, len, EnhanceResult.SAMPLE_RATE, opt.silenceCut)
            voice = r.pcm; len = r.length; removedSilence = r.removedSamples
        }
        if (voice.size != len) voice = voice.copyOf(len)

        val report = if (len > 0) NativePolish.run(voice, len, opt.preset, opt.normalize, opt.targetLufs) else null

        var original: FloatArray? = null
        var removedStem: FloatArray? = null
        if (wantStems) {
            // Level-match the stems to the final voice so A/B listening compares noise, not loudness.
            val target = if (report != null) report.outputLufs else NativePolish.lufs(voice, len)
            original = levelMatched(x, n, target)
            removedStem = levelMatched(removed!!, n, min(target, -20f))
        }
        onProgress?.invoke(1f)
        val ms = (System.nanoTime() - t0) / 1_000_000
        AudioLog.lastEngine = engineUsed.name
        AudioLog.i(TAG, "processed ${"%.1f".format(inputSeconds)}s in $ms ms (${"%.1f".format(inputSeconds * 1000f / max(1L, ms))}x realtime) engine=$engineUsed")
        return EnhanceResult(voice, len, original, removedStem, engineUsed, ms, inputSeconds, removedSilence / EnhanceResult.SAMPLE_RATE.toFloat(), report)
    }

    private fun classic(x: FloatArray, n: Int, opt: EnhanceOptions): FloatArray {
        val s16 = Resampler.toShort(x, n)
        val strength = (0.2f + 0.8f * (min(opt.attenuationDb, 80f) - 6f) / 74f).coerceIn(0.2f, 1f)
        NativeDenoiser(EnhanceResult.SAMPLE_RATE).use { d ->
            val out = d.process(s16, n, strength, null, 0f, false)
            return Resampler.toFloat(out, n)
        }
    }

    private fun levelMatched(x: FloatArray, n: Int, targetLufs: Float): FloatArray {
        val out = x.copyOf(n)
        val cur = NativePolish.lufs(out, n)
        if (cur > -69f) {
            val g = Math.pow(10.0, (targetLufs - cur).coerceIn(-24f, 30f) / 20.0).toFloat()
            for (i in 0 until n) out[i] = (out[i] * g).coerceIn(-0.98f, 0.98f)
        }
        return out
    }

    companion object { private const val TAG = "EnhancePipeline" }
}
