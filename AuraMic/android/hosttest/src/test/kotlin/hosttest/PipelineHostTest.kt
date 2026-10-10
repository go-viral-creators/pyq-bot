package hosttest

import com.kaafisir.auramic.audio.DfnRuntime
import com.kaafisir.auramic.audio.EnhanceResult
import com.kaafisir.auramic.audio.EnhancementPipeline
import com.kaafisir.auramic.audio.GtcrnRuntime
import com.kaafisir.auramic.audio.NativePolish
import com.kaafisir.auramic.audio.SilenceCutter
import com.kaafisir.auramic.audio.Resampler
import com.kaafisir.auramic.domain.model.EnhanceEngine
import com.kaafisir.auramic.domain.model.EnhanceOptions
import com.kaafisir.auramic.domain.model.SilenceCut
import com.kaafisir.auramic.domain.model.VoicePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class PipelineHostTest {
    companion object {
        lateinit var noisy: ShortArray
        var clean: ShortArray? = null
        lateinit var runtime: DfnRuntime
        lateinit var gtcrn: GtcrnRuntime
        lateinit var pipeline: EnhancementPipeline
        var ready = false

        /** Minimal 16-bit PCM (or float32) mono WAV reader. */
        fun readWav(path: String): Pair<ShortArray, Int> {
            val b = ByteBuffer.wrap(File(path).readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            var pos = 12; var fmt = 1; var ch = 1; var sr = 48_000; var bits = 16
            while (pos + 8 <= b.capacity()) {
                val id = String(ByteArray(4) { b.get(pos + it) }); val size = b.getInt(pos + 4)
                if (id == "fmt ") { fmt = b.getShort(pos + 8).toInt(); ch = b.getShort(pos + 10).toInt(); sr = b.getInt(pos + 12); bits = b.getShort(pos + 22).toInt() }
                if (id == "data") {
                    val n = size / (bits / 8) / ch
                    val out = ShortArray(n) { i ->
                        val o = pos + 8 + i * ch * (bits / 8)
                        if (fmt == 3) (b.getFloat(o).coerceIn(-1f, 1f) * 32767).toInt().toShort() else b.getShort(o)
                    }
                    return out to sr
                }
                pos += 8 + size + (size and 1)
            }
            error("no data chunk")
        }

        @JvmStatic @BeforeClass fun setup() {
            val sample = System.getProperty("auramic.sample")
            val libDir = System.getProperty("java.library.path")
            if (sample == null || libDir.isNullOrBlank() || !File(sample).exists()) return
            try { System.loadLibrary("auramic_dsp") } catch (e: UnsatisfiedLinkError) { println("host JNI lib not found: $e"); return }
            val (pcm, sr) = readWav(sample)
            assertEquals(48_000, sr)
            noisy = pcm
            System.getProperty("auramic.clean")?.takeIf { File(it).exists() }?.let { clean = readWav(it).first }
            val models = File(System.getProperty("auramic.models"))
            runtime = DfnRuntime(4) { File(models, "$it.ort").readBytes() }
            gtcrn = GtcrnRuntime(4) { File(System.getProperty("auramic.gtcrn")).readBytes() }
            pipeline = EnhancementPipeline(runtime, gtcrn)
            ready = true
        }

        fun frameDb(x: FloatArray, n: Int): FloatArray {
            val f = 480
            return FloatArray(n / f) { k -> var e = 0.0; for (i in k * f until (k + 1) * f) e += x[i].toDouble() * x[i]; (10 * log10(e / f + 1e-12)).toFloat() }
        }

        /** loud-vs-quiet frame level difference: a proxy for how far the noise floor sits below the speech. */
        fun dynamicRangeDb(x: FloatArray, n: Int): Float {
            val s = frameDb(x, n).sortedArray()
            val q = s.take(s.size / 10).average(); val l = s.takeLast(s.size / 10).average()
            return (l - q).toFloat()
        }
    }

    private val defaultOpt = EnhanceOptions(preset = VoicePreset.OFF, normalize = false, silenceCut = SilenceCut.OFF)

    @Test fun networkOutputHasInputLengthAndRemovesNoise() {
        assumeTrue("host lib + sample not configured", ready)
        val x = Resampler.toFloat(noisy, noisy.size)
        val y = runtime.enhance(x, x.size, 100f, false)
        assertNotNull(y)
        assertEquals(x.size, y!!.size)
        assertTrue("output must be finite", y.all { it.isFinite() })
        val before = dynamicRangeDb(x, x.size); val after = dynamicRangeDb(y, y.size)
        println("dynamic range (speech vs noise floor): $before dB -> $after dB")
        assertTrue("noise floor should drop by >15 dB (was ${after - before})", after - before > 15f)
    }

    @Test fun matchesPythonReferenceWhenAvailable() {
        assumeTrue(ready)
        val ref = System.getProperty("auramic.reference")?.takeIf { File(it).exists() }
        assumeNotNull(ref)
        val (r, _) = readWav(ref!!)
        val x = Resampler.toFloat(noisy, noisy.size)
        val y = runtime.enhance(x, x.size, 100f, false)!!
        var num = 0.0; var den = 0.0
        for (i in y.indices) { val d = y[i] - r[i] / 32768.0; num += d * d; den += (r[i] / 32768.0) * (r[i] / 32768.0) }
        val rel = sqrt(num / den)
        println("Kotlin vs python mirror relative RMS error: $rel")
        assertTrue("relative error $rel", rel < 0.02)
    }

    @Test fun fastModeAndAttenuationLimitBehave() {
        assumeTrue(ready)
        val x = Resampler.toFloat(noisy, noisy.size)
        val full = runtime.enhance(x, x.size, 100f, false)!!
        val fast = runtime.enhance(x, x.size, 100f, true)!!
        val limited = runtime.enhance(x, x.size, 12f, false)!!
        val drFull = dynamicRangeDb(full, full.size); val drLim = dynamicRangeDb(limited, limited.size)
        println("dynamic range: full $drFull, fast ${dynamicRangeDb(fast, fast.size)}, 12 dB limit $drLim")
        assertTrue("limited attenuation must keep more ambience", drLim < drFull - 10f)
        assertTrue(dynamicRangeDb(fast, fast.size) > 15f)
    }

    @Test fun fullPipelineMastersToTargetLoudness() {
        assumeTrue(ready)
        val r = pipeline.process(noisy, noisy.size, 48_000, EnhanceOptions(silenceCut = SilenceCut.OFF, preset = VoicePreset.PODCAST, normalize = true, targetLufs = -16f), wantStems = true)
        println("pipeline: ${r.processingMs} ms for ${r.inputSeconds} s (${r.speedFactor}x realtime), engine=${r.engineUsed}, ${r.report?.inputLufs} -> ${r.report?.outputLufs} LUFS, peak ${r.report?.peakDb} dBFS")
        assertEquals(EnhanceEngine.AI, r.engineUsed)
        assertEquals(noisy.size, r.length)
        assertEquals(-16f, r.report!!.outputLufs, 1.0f)
        assertTrue(r.report!!.peakDb <= -1.0f)
        assertTrue("stems requested", r.original != null && r.removed != null)
        // the removed stem plus the clean voice should reconstruct (roughly) the original energy ordering
        assertTrue(NativePolish.lufs(r.removed!!, r.length) > -70f)
    }

    @Test fun silenceCutShortensAndResamplePathWorks() {
        assumeTrue(ready)
        // 44.1 kHz input path: resample down then run the whole pipeline
        val x = Resampler.toFloat(noisy, noisy.size)
        val x441 = Resampler.resample(x, x.size, 48_000, 44_100)
        val pcm441 = Resampler.toShort(x441, x441.size)
        val r = pipeline.process(pcm441, pcm441.size, 44_100, EnhanceOptions(silenceCut = SilenceCut.TIGHT, preset = VoicePreset.NATURAL))
        println("44.1k + tight cut: ${r.inputSeconds} s in -> ${r.seconds} s out (removed ${r.removedSilenceSeconds} s)")
        assertEquals(r.inputSeconds, noisy.size / 48_000f, 0.05f)
        assertTrue(r.length < (noisy.size * 0.98).toInt())
        assertTrue(r.voice.take(r.length).all { abs(it) <= 1f })
    }

    @Test fun classicEngineStillWorksAsFallback() {
        assumeTrue(ready)
        val r = pipeline.process(noisy, noisy.size, 48_000, EnhanceOptions(engine = EnhanceEngine.CLASSIC, preset = VoicePreset.OFF, normalize = false, silenceCut = SilenceCut.OFF))
        assertEquals(EnhanceEngine.CLASSIC, r.engineUsed)
        assertEquals(noisy.size, r.length)
    }

    @Test fun tinyAndSilentInputsAreSafe() {
        assumeTrue(ready)
        for (n in intArrayOf(1, 100, 481, 4800)) {
            val r = pipeline.process(ShortArray(n), n, 48_000, EnhanceOptions(silenceCut = SilenceCut.OFF))
            pipeline.process(ShortArray(n), n, 48_000, EnhanceOptions(engine = EnhanceEngine.LIGHT))
            assertEquals(n, r.length)
        }
    }

    private fun corr(a: FloatArray, b: FloatArray): Double {
        val n = minOf(a.size, b.size); var ab = 0.0; var aa = 0.0; var bb = 0.0
        for (i in 0 until n) { ab += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i] }
        return ab / sqrt(aa * bb + 1e-30)
    }

    @Test fun deepFilterNetMatchesTheOfficialPyTorchModel() {
        assumeTrue(ready)
        val ref = System.getProperty("auramic.official")?.takeIf { File(it).exists() }
        assumeNotNull(ref)
        val r = readWav(ref!!).first.let { Resampler.toFloat(it, it.size) }
        val y = pipeline.process(noisy, noisy.size, 48_000, EnhanceOptions(silenceCut = SilenceCut.OFF, preset = VoicePreset.OFF, normalize = false)).voice
        val c = corr(y, r); val dl = 10 * log10(y.sumOf { it.toDouble() * it } / r.sumOf { it.toDouble() * it })
        println("DeepFilterNet3 vs official: corr $c, level diff $dl dB")
        assertTrue("corr $c", c > 0.99); assertTrue("level $dl", abs(dl) < 0.5)
    }

    @Test fun gtcrnMatchesTheOfficialPyTorchModel() {
        assumeTrue(ready)
        val ref = System.getProperty("auramic.gtcrnref")?.takeIf { File(it).exists() }
        assumeNotNull(ref)
        val r = readWav(ref!!).first.let { Resampler.toFloat(it, it.size) }
        val x = Resampler.toFloat(noisy, noisy.size)
        val t0 = System.nanoTime()
        val y = gtcrn.enhance(x, x.size)!!
        println("GTCRN: ${(System.nanoTime() - t0) / 1_000_000} ms for ${x.size / 48000f} s")
        assertEquals(x.size, y.size)
        val c = corr(y, r); val dl = 10 * log10(y.sumOf { it.toDouble() * it } / r.sumOf { it.toDouble() * it })
        println("GTCRN vs official: corr $c, level diff $dl dB")
        assertTrue("corr $c", c > 0.97); assertTrue("level $dl", abs(dl) < 1.0)
    }

    @Test fun mediumPipelineHasNoPauseLongerThanThreeHundredMs() {
        assumeTrue(ready)
        val r = pipeline.process(noisy, noisy.size, 48_000, EnhanceOptions.forQuality(com.kaafisir.auramic.domain.model.Quality.MEDIUM))
        assertEquals(EnhanceEngine.LIGHT, r.engineUsed)
        val db = frameDb(r.voice, r.length)
        val ref = db.sortedArray()[(db.size * 0.9).toInt()]
        var run = 0; var longest = 0
        for (v in db) { if (v < ref - 40f) { run++; longest = maxOf(longest, run) } else run = 0 }
        println("Medium: ${r.inputSeconds} s -> ${r.seconds} s, longest quiet run ${longest * 10} ms, ${r.speedFactor}x realtime")
        assertTrue("longest pause ${longest * 10} ms", longest * 10 <= 330)
    }

    /** With -Dauramic.dump=<dir> (gradle: -Pauramic.dump) writes what the app would send, for listening tests. */
    @Test fun dumpFinalOutputsForListening() {
        assumeTrue(ready)
        val dir = System.getProperty("auramic.dump")?.takeIf { it.isNotBlank() } ?: return
        File(dir).mkdirs()
        for (q in com.kaafisir.auramic.domain.model.Quality.entries) {
            val r = pipeline.process(noisy, noisy.size, 48_000, EnhanceOptions.forQuality(q))
            val pcm = Resampler.toShort(r.voice, r.length)
            val bb = ByteBuffer.allocate(44 + pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            bb.put("RIFF".toByteArray()).putInt(36 + pcm.size * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(48_000).putInt(96_000).putShort(2).putShort(16).put("data".toByteArray()).putInt(pcm.size * 2)
            pcm.forEach { bb.putShort(it) }
            File(dir, "app_${q.name}.wav").writeBytes(bb.array())
            println("dumped ${q.name}: ${r.seconds} s, engine ${r.engineUsed}, ${r.speedFactor}x")
        }
    }

    /**
     * The tail of the app pipeline (auto-cut pauses + podcast mastering) applied to already-denoised wav files, for
     * experiments: -Pauramic.tailjobs=<file with "in.wav out.wav" lines>. Output is 16-bit mono 48 kHz.
     */
    @Test fun runTailJobs() {
        assumeTrue(ready)
        val jobs = System.getProperty("auramic.tailjobs")?.takeIf { File(it).exists() } ?: return
        for (line in File(jobs).readLines().filter { it.isNotBlank() }) {
            val parts = line.trim().split(" "); val inp = parts[0]; val out = parts[1]; val cutOnly = parts.getOrNull(2) == "cut"
            val (pcm, sr) = readWav(inp)
            assertEquals(48_000, sr)
            var voice = Resampler.toFloat(pcm, pcm.size); var len = voice.size
            val r = SilenceCutter.cut(voice, len, 48_000, SilenceCut.PAUSES)
            voice = r.pcm; len = r.length
            if (voice.size != len) voice = voice.copyOf(len)
            if (!cutOnly) NativePolish.run(voice, len, VoicePreset.PODCAST, true, -16f)
            val o = Resampler.toShort(voice, len)
            val bb = ByteBuffer.allocate(44 + o.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            bb.put("RIFF".toByteArray()).putInt(36 + o.size * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(48_000).putInt(96_000).putShort(2).putShort(16).put("data".toByteArray()).putInt(o.size * 2)
            o.forEach { bb.putShort(it) }
            File(out).writeBytes(bb.array())
            println("tail: $inp -> $out (${len / 48000f} s)")
        }
    }

    private fun assertNotNull(o: Any?) = org.junit.Assert.assertNotNull(o)
}
