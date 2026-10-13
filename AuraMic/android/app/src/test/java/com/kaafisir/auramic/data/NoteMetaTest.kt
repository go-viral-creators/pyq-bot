package com.kaafisir.auramic.data

import com.kaafisir.auramic.data.notes.NoteArchive
import com.kaafisir.auramic.data.notes.NoteMeta
import com.kaafisir.auramic.data.notes.peaksOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files

class NoteMetaTest {
    private fun tempDir(): File = Files.createTempDirectory("note").toFile()

    @Test fun roundTripWithBothTakes() {
        val dir = File(tempDir(), "1760000000000").also { it.mkdirs() }
        File(dir, NoteArchive.ORIGINAL).writeBytes(ByteArray(100))
        File(dir, NoteArchive.ENHANCED).writeBytes(ByteArray(60))
        File(dir, NoteMeta.FILE).writeText(NoteMeta.write(1760000000000, 61_000, 52_500, floatArrayOf(0.1f, 0.5f), floatArrayOf(0.2f)))
        val n = NoteMeta.read(dir)!!
        assertEquals("1760000000000", n.id)
        assertEquals(1760000000000, n.createdAt)
        assertEquals(61_000, n.originalMs); assertEquals(52_500, n.enhancedMs)
        assertNotNull(n.originalFile); assertNotNull(n.enhancedFile)
        assertEquals(2, n.originalPeaks.size); assertEquals(0.5f, n.originalPeaks[1], 1e-3f)
        assertEquals(160 + File(dir, NoteMeta.FILE).length(), n.bytes)
    }

    @Test fun missingTakeIsReportedNotHidden() {
        val dir = File(tempDir(), "5").also { it.mkdirs() }
        File(dir, NoteArchive.ENHANCED).writeBytes(ByteArray(10))
        File(dir, NoteMeta.FILE).writeText(NoteMeta.write(5, 1000, 900, FloatArray(0), FloatArray(0)))
        val n = NoteMeta.read(dir)!!
        assertNull(n.originalFile)
        assertNotNull(n.enhancedFile)
    }

    @Test fun unreadableMetaIsSkipped() {
        val dir = File(tempDir(), "7").also { it.mkdirs() }
        File(dir, NoteMeta.FILE).writeText("garbage")
        assertNull(NoteMeta.read(dir))
    }

    @Test fun peaksFollowTheSignal() {
        val x = FloatArray(48_000) { if (it < 24_000) 0.01f else 0.8f }
        val p = peaksOf(x.size, 10) { x[it] }
        assertEquals(10, p.size)
        assertEquals(0.01f, p[0], 1e-4f)
        assertEquals(0.8f, p[9], 1e-4f)
        assertEquals(0, peaksOf(0) { 0f }.size)
    }
}
