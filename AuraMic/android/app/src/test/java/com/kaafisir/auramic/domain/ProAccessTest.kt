package com.kaafisir.auramic.domain

import com.kaafisir.auramic.data.pro.FREE_HIGH_NOTES
import com.kaafisir.auramic.data.pro.ProState
import com.kaafisir.auramic.data.pro.TrialCounter
import com.kaafisir.auramic.data.pro.TrialSource
import com.kaafisir.auramic.data.pro.resolveQuality
import com.kaafisir.auramic.domain.model.Quality
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class MemSource(var value: String? = null) : TrialSource {
    override suspend fun read() = value
    override suspend fun write(value: String) { this.value = value }
}

class ProAccessTest {
    private val key = ByteArray(32) { it.toByte() }

    @Test fun freeUserDefaultsToMediumAndCanOnlyUseHighWhileTriesRemain() {
        assertEquals(Quality.MEDIUM, resolveQuality(null, ProState()))
        assertEquals(Quality.HIGH, resolveQuality(Quality.HIGH, ProState(trialUsed = 3)))
        assertEquals(Quality.MEDIUM, resolveQuality(Quality.HIGH, ProState(trialUsed = FREE_HIGH_NOTES)))
        assertEquals(Quality.MEDIUM, resolveQuality(Quality.MEDIUM, ProState()))
    }

    @Test fun subscriberDefaultsToHighButMayPickMedium() {
        val pro = ProState(subscribed = true, trialUsed = FREE_HIGH_NOTES)
        assertEquals(Quality.HIGH, resolveQuality(null, pro))
        assertEquals(Quality.MEDIUM, resolveQuality(Quality.MEDIUM, pro))
    }

    @Test fun tenFreeTriesThenNoMore() = runBlocking {
        val c = TrialCounter(listOf(MemSource()), key)
        assertEquals(0, c.used())
        repeat(FREE_HIGH_NOTES + 3) { c.increment() }
        assertEquals(FREE_HIGH_NOTES, c.used())
        assertEquals(0, ProState(trialUsed = c.used()).trialLeft)
    }

    @Test fun clearingOneStoreDoesNotResetTheCount() = runBlocking {
        val a = MemSource(); val b = MemSource()
        val c = TrialCounter(listOf(a, b), key)
        repeat(4) { c.increment() }
        a.value = null                       // user clears app data: the DataStore copy disappears
        assertEquals(4, c.used())            // the shared-storage copy still knows
        assertEquals(c.encode(4), a.value)   // ...and heals the cleared copy
    }

    @Test fun forgedOrCorruptedCounterCountsAsUsedUp() = runBlocking {
        val a = MemSource("v1|0|deadbeefdeadbeef")
        assertEquals(FREE_HIGH_NOTES, TrialCounter(listOf(a), key).used())
    }

    @Test fun counterSignedWithAnotherKeyIsRejected() = runBlocking {
        val other = TrialCounter(listOf(MemSource()), ByteArray(32) { 9 })
        val a = MemSource(other.encode(0))
        assertEquals(FREE_HIGH_NOTES, TrialCounter(listOf(a), key).used())
    }
}
