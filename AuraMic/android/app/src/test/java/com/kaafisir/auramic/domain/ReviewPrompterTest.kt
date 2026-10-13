package com.kaafisir.auramic.domain

import com.kaafisir.auramic.data.review.ReviewPrompter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPrompterTest {
    private val day = 86_400_000L
    private val now = 1_790_000_000_000L

    @Test fun asksOnlyAfterEnoughNotesAndNotTooOften() {
        assertFalse(ReviewPrompter.shouldAsk(4, 0, now))
        assertTrue(ReviewPrompter.shouldAsk(5, 0, now))
        assertFalse(ReviewPrompter.shouldAsk(50, now - 30 * day, now))
        assertTrue(ReviewPrompter.shouldAsk(50, now - 121 * day, now))
    }
}
