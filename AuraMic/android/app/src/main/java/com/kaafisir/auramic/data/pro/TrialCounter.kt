package com.kaafisir.auramic.data.pro

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** One place the free-trial counter is stored. Values are opaque signed strings. */
interface TrialSource {
    suspend fun read(): String?
    suspend fun write(value: String)
}

/**
 * Counts the free High Quality notes without a server. The count is kept in several places that survive
 * "Clear data" differently (backed-up DataStore, a signed file in shared storage owned by the app); the highest
 * value wins and is written back everywhere, so clearing app data cannot reset the trial.
 * A value that fails its signature check counts as "fully used".
 */
class TrialCounter(private val sources: List<TrialSource>, private val key: ByteArray) {

    suspend fun used(): Int {
        var best = 0
        for (src in sources) {
            val raw = runCatching { src.read() }.getOrNull()
            // nothing stored = 0 used; a value that fails its signature = fully used
            val v = if (raw == null) 0 else decode(raw) ?: FREE_HIGH_NOTES
            best = maxOf(best, v)
        }
        best = best.coerceIn(0, FREE_HIGH_NOTES)
        heal(best)
        return best
    }

    suspend fun increment(): Int {
        val next = (used() + 1).coerceAtMost(FREE_HIGH_NOTES)
        heal(next, force = true)
        return next
    }

    private suspend fun heal(value: Int, force: Boolean = false) {
        if (value == 0 && !force) return
        val enc = encode(value)
        for (s in sources) {
            val cur = runCatching { s.read() }.getOrNull()
            if (force || cur != enc) runCatching { s.write(enc) }
        }
    }

    fun encode(used: Int): String = "v1|$used|${mac("v1|$used")}"

    fun decode(raw: String): Int? {
        val p = raw.trim().split('|')
        if (p.size != 3 || p[0] != "v1") return null
        val n = p[1].toIntOrNull() ?: return null
        return if (mac("v1|$n") == p[2]) n else null
    }

    private fun mac(msg: String): String {
        val m = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
        return m.doFinal(msg.toByteArray()).take(8).joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Device-bound signing key (ANDROID_ID is stable across "Clear data" and per app-signing key). */
        fun keyFor(context: Context): ByteArray {
            val id = runCatching { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }.getOrNull().orEmpty()
            return MessageDigest.getInstance("SHA-256").digest("auramic.trial.v1|${context.packageName}|$id".toByteArray())
        }
    }
}

/** App DataStore copy (included in Android backup, so it also survives reinstall via Google backup). */
class DataStoreTrialSource(private val store: DataStore<Preferences>) : TrialSource {
    private val k = stringPreferencesKey("trial_state")
    override suspend fun read(): String? = store.data.first()[k]
    override suspend fun write(value: String) { store.edit { it[k] = value } }
}

/**
 * A small signed file in shared storage (Documents/AuraMic/license.dat). Files an app creates through MediaStore stay owned by
 * it, so the app can still read and update them after the user clears its data (Android 10+; no permission needed).
 */
class MediaStoreTrialSource(private val context: Context) : TrialSource {
    private val collection: Uri get() = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private fun find(): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        context.contentResolver.query(
            collection, arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
            arrayOf(NAME, "$DIR%"), null,
        )?.use { c -> if (c.moveToFirst()) return android.content.ContentUris.withAppendedId(collection, c.getLong(0)) }
        return null
    }

    override suspend fun read(): String? {
        if (Build.VERSION.SDK_INT < 29) return null
        val uri = find() ?: return null
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    override suspend fun write(value: String) {
        if (Build.VERSION.SDK_INT < 29) return
        val uri = find() ?: context.contentResolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, DIR)
        }) ?: return
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(value.toByteArray()) }
    }

    private companion object {
        const val NAME = "license.dat"
        const val DIR = "Documents/AuraMic/"
    }
}
