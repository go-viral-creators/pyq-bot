package com.kaafisir.auramic.domain

import com.kaafisir.auramic.data.pro.ProAllowlist
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProAllowlistTest {
    @Test fun ownDeviceIsAllowedOthersAreNot() {
        assertTrue(ProAllowlist.isAllowedId("92ae6fd1c66039c6"))
        assertTrue(ProAllowlist.isAllowedId(" 92AE6FD1C66039C6 "))
        assertFalse(ProAllowlist.isAllowedId("0123456789abcdef"))
        assertFalse(ProAllowlist.isAllowedId(""))
    }
}
