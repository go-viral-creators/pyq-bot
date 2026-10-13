package com.kaafisir.auramic.data.notes

import android.content.Context
import android.util.Log
import com.kaafisir.auramic.audio.AudioEncoder
import com.kaafisir.auramic.audio.PcmRecorder
import com.kaafisir.auramic.domain.model.OutputFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Save original and enhanced audio": both versions of every voice note sent through the floating mic, kept in the
 * app's private storage (no permission needed, removed with the app, never uploaded).
 *
 * Layout: files/notes/<createdAt>/{original.m4a, enhanced.m4a, meta.txt}. A note is written into a temporary folder
 * and renamed into place, so a half-written note is never listed. Nothing is ever deleted except on the user's request.
 */
@Singleton
class NoteArchive @Inject constructor(@ApplicationContext private val context: Context) {

    sealed interface State {
        data object Loading : State
        data class Loaded(val notes: List<SavedNote>, val totalBytes: Long) : State
    }

    private val root: File get() = File(context.filesDir, DIR)
    private val lock = Mutex()
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Re-reads the folder (newest first). */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        lock.withLock { publish() }
    }

    private fun publish() {
        val notes = root.listFiles { f -> f.isDirectory && !f.name.startsWith(TMP) }.orEmpty()
            .mapNotNull { NoteMeta.read(it) }
            .sortedByDescending { it.createdAt }
        _state.value = State.Loaded(notes, notes.sumOf { it.bytes })
    }

    /**
     * Saves [original] (encoded here) and a copy of [enhancedFile]. Returns true when at least one take was saved.
     * Failures are logged and leave nothing behind; they never affect sending.
     */
    suspend fun save(
        original: PcmRecorder.Recording,
        enhancedFile: File,
        enhancedMs: Long,
        enhancedPeaks: FloatArray,
        createdAt: Long = System.currentTimeMillis(),
    ): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            val tmp = File(root, "$TMP$createdAt")
            try {
                tmp.deleteRecursively(); tmp.mkdirs()
                // Space Android can actually give us (counts cache it may clear), not just the raw free bytes.
                val free = runCatching {
                    val sm = context.getSystemService(android.os.storage.StorageManager::class.java)
                    sm.getAllocatableBytes(sm.getUuidForPath(tmp))
                }.getOrDefault(tmp.usableSpace)
                if (free < MIN_FREE_BYTES) { Log.w(TAG, "not enough space, note not saved"); tmp.deleteRecursively(); return@withLock false }
                val origOk = runCatching {
                    val enc = AudioEncoder.encode(original.pcm, original.length, original.sampleRate, OutputFormat.M4A, tmp)
                    enc.file.renameTo(File(tmp, ORIGINAL)) || throw IllegalStateException("rename failed")
                }.onFailure { Log.w(TAG, "original not saved", it) }.isSuccess
                val enhOk = runCatching { enhancedFile.copyTo(File(tmp, ENHANCED), overwrite = true); true }
                    .onFailure { Log.w(TAG, "enhanced not saved", it) }.isSuccess
                if (!origOk && !enhOk) { tmp.deleteRecursively(); return@withLock false }
                val origPeaks = peaksOf(original.length) { original.pcm[it] / 32768f }
                File(tmp, NoteMeta.FILE).writeText(
                    NoteMeta.write(createdAt, (original.seconds * 1000).toLong(), enhancedMs, origPeaks, enhancedPeaks),
                )
                val dest = File(root, createdAt.toString())
                dest.deleteRecursively()
                if (!tmp.renameTo(dest)) { tmp.deleteRecursively(); return@withLock false }
                publish()
                true
            } catch (t: Throwable) {
                Log.w(TAG, "note not saved", t)
                runCatching { tmp.deleteRecursively() }
                false
            }
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            if (id.isNotBlank() && !id.contains('/') && !id.contains("..")) File(root, id).deleteRecursively()
            publish()
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        lock.withLock {
            root.listFiles()?.forEach { it.deleteRecursively() }
            publish()
        }
    }

    /** Removes temporary folders left behind if the app died while saving (they were never listed). */
    suspend fun cleanUpInterrupted() = withContext(Dispatchers.IO) {
        lock.withLock { root.listFiles { f -> f.name.startsWith(TMP) }?.forEach { it.deleteRecursively() } }
    }

    companion object {
        private const val TAG = "NoteArchive"
        const val DIR = "notes"
        const val ORIGINAL = "original.m4a"
        const val ENHANCED = "enhanced.m4a"
        private const val TMP = ".tmp-"
        /** Keep at least this much free so saving notes can never fill the phone. */
        private const val MIN_FREE_BYTES = 50L * 1024 * 1024
    }
}
