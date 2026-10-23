package com.kaafisir.auramic.data

import com.kaafisir.auramic.data.permissions.KeepRunningGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepRunningGuideTest {
    private fun family(m: String, b: String = m) = KeepRunningGuide.forDevice(m, b).family

    @Test fun recognisesTheCommonMakersAndTheirSubBrands() {
        assertEquals("Xiaomi / Redmi / POCO", family("Xiaomi", "POCO"))
        assertEquals("Xiaomi / Redmi / POCO", family("Xiaomi", "Redmi"))
        assertEquals("Realme", family("realme"))
        assertEquals("OnePlus", family("OnePlus"))
        assertEquals("Oppo", family("OPPO"))
        assertEquals("Vivo / iQOO", family("vivo", "iQOO"))
        assertEquals("Samsung", family("samsung"))
        assertEquals("Huawei / Honor", family("HUAWEI", "HONOR"))
        assertEquals("Tecno / Infinix / itel", family("TECNO MOBILE LIMITED", "TECNO"))
        assertEquals("Tecno / Infinix / itel", family("INFINIX", "Infinix"))
        assertEquals("Motorola", family("motorola"))
        assertEquals("Asus", family("ASUS"))
        assertEquals("Android", family("Google", "google"))
        assertEquals("Android", family("Nothing"))
    }

    @Test fun unknownMakersGetTheGenericGuide() {
        val g = KeepRunningGuide.forDevice("Lava", "Lava")
        assertEquals("your phone", g.family)
        assertTrue(g.steps.size >= 2)
    }

    @Test fun nullsDoNotCrash() = assertEquals("your phone", KeepRunningGuide.forDevice(null, null).family)

    @Test fun everyGuideHasStepsAndTheAutostartMakersHaveTargets() {
        val makers = listOf("Xiaomi", "realme", "OnePlus", "OPPO", "vivo", "samsung", "HUAWEI", "TECNO", "motorola", "ASUS", "lenovo", "meizu", "ZTE", "LGE", "Google", "Lava")
        for (m in makers) assertTrue("$m has steps", KeepRunningGuide.forDevice(m, m).steps.size >= 2)
        for (m in listOf("Xiaomi", "OPPO", "vivo", "HUAWEI", "TECNO", "ASUS")) {
            assertTrue("$m has autostart pages", KeepRunningGuide.forDevice(m, m).autostartTargets.isNotEmpty())
        }
    }
}
