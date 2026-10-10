package com.kaafisir.auramic.automation

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps a copy of a processed note in Music/AuraMic (fallback when an app cannot receive audio, or on request). */
@Singleton
class MediaSaver @Inject constructor(@ApplicationContext private val context: Context) {
    fun saveToMusic(file: File, mime: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Audio.Media.MIME_TYPE, mime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/AuraMic")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) false else {
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
                true
            }
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return false
            file.copyTo(File(dir, file.name), overwrite = true)
            true
        }
    } catch (t: Throwable) {
        Log.w("MediaSaver", "save failed", t)
        false
    }
}
