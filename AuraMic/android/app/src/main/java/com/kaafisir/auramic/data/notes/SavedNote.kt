package com.kaafisir.auramic.data.notes

import java.io.File

/**
 * One voice note sent through the floating mic, as kept for Studio Lab: the recording as spoken and the cleaned
 * version that went to WhatsApp. Either file can be missing when saving it failed ([originalFile] / [enhancedFile] null).
 */
data class SavedNote(
    val id: String,
    val createdAt: Long,
    val originalFile: File?,
    val enhancedFile: File?,
    val originalMs: Long,
    val enhancedMs: Long,
    /** ~120 peak values 0..1 per take, for the waveform (no decoding needed to draw it). */
    val originalPeaks: FloatArray,
    val enhancedPeaks: FloatArray,
    val bytes: Long,
) {
    override fun equals(other: Any?) = other is SavedNote && other.id == id && other.bytes == bytes
    override fun hashCode() = id.hashCode()
}

/** Plain "key=value" meta file (no JSON dependency, so it is testable on the JVM). */
internal object NoteMeta {
    const val FILE = "meta.txt"

    fun write(createdAt: Long, originalMs: Long, enhancedMs: Long, originalPeaks: FloatArray, enhancedPeaks: FloatArray): String =
        buildString {
            appendLine("v=1")
            appendLine("createdAt=$createdAt")
            appendLine("originalMs=$originalMs")
            appendLine("enhancedMs=$enhancedMs")
            appendLine("originalPeaks=" + originalPeaks.joinToString(",") { "%.3f".format(java.util.Locale.US, it) })
            appendLine("enhancedPeaks=" + enhancedPeaks.joinToString(",") { "%.3f".format(java.util.Locale.US, it) })
        }

    /** Null when the meta file is unreadable (the note is then skipped, never deleted). */
    fun read(dir: File): SavedNote? = runCatching {
        val kv = File(dir, FILE).readLines().mapNotNull { l -> l.indexOf('=').takeIf { it > 0 }?.let { l.substring(0, it) to l.substring(it + 1) } }.toMap()
        fun peaks(k: String) = kv[k]?.takeIf { it.isNotBlank() }?.split(",")?.map { it.toFloat() }?.toFloatArray() ?: FloatArray(0)
        val orig = File(dir, NoteArchive.ORIGINAL).takeIf { it.isFile && it.length() > 0 }
        val enh = File(dir, NoteArchive.ENHANCED).takeIf { it.isFile && it.length() > 0 }
        SavedNote(
            id = dir.name,
            createdAt = kv.getValue("createdAt").toLong(),
            originalFile = orig, enhancedFile = enh,
            originalMs = kv["originalMs"]?.toLong() ?: 0, enhancedMs = kv["enhancedMs"]?.toLong() ?: 0,
            originalPeaks = peaks("originalPeaks"), enhancedPeaks = peaks("enhancedPeaks"),
            bytes = dir.listFiles()?.sumOf { it.length() } ?: 0,
        )
    }.getOrNull()
}

/** [count] peak values (0..1) of a mono signal, for a compact waveform. */
fun peaksOf(length: Int, count: Int = 120, sample: (Int) -> Float): FloatArray {
    if (length <= 0) return FloatArray(0)
    val out = FloatArray(count)
    for (b in 0 until count) {
        val from = (b.toLong() * length / count).toInt()
        val to = maxOf(from + 1, ((b + 1).toLong() * length / count).toInt()).coerceAtMost(length)
        var m = 0f
        var i = from
        val step = maxOf(1, (to - from) / 256)          // a few hundred reads per bar is plenty for a peak meter
        while (i < to) { val v = kotlin.math.abs(sample(i)); if (v > m) m = v; i += step }
        out[b] = m.coerceIn(0f, 1f)
    }
    return out
}
