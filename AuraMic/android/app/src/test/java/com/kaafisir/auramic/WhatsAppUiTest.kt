package com.kaafisir.auramic

import com.kaafisir.auramic.automation.WhatsAppUi
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppUiTest {
    @Test fun normalizeStripsAccentsCaseAndMarks() {
        assertEquals("jose maria", WhatsAppUi.normalize("  JOSÉ   María "))
        assertEquals("abc", WhatsAppUi.normalize("‏abc‎"))
        assertEquals("", WhatsAppUi.normalize(null))
    }
}
