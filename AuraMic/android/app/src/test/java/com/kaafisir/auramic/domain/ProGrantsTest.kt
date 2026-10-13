package com.kaafisir.auramic.domain

import com.kaafisir.auramic.data.pro.GrantCheck
import com.kaafisir.auramic.data.pro.GrantCodec
import com.kaafisir.auramic.data.pro.ProState
import com.kaafisir.auramic.data.pro.resolveQuality
import com.kaafisir.auramic.domain.model.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class ProGrantsTest {
    // Signed with the real admin key by tools/pro_grants.py; both expired on 1 Jan 2020 (safe to keep in the repo).
    private val unboundExpired = "AURA-AEAAM-6JSI5-LAAAA-AABC4-UEEZY-FSXSP-4ZZLN-BXWFZ-O37RJ-RAMJZ-SYEU5-G4OLL-C774M-7XZ64-77R7C-6S7OS-4ZU7W-SE3PT-PMVYW-CX45O-PK4BS-G7N3K-ALS7D-JZOGW"
    private val boundExpired = "AURA-AEAAM-6JTI5-LLZGS-QENDK-5X2KQ-OABR4-TINWS-WNZQ3-PZQTH-4EU2V-4E7GQ-DRRHG-K2EP2-5NVTC-4KNI2-HV3X7-XWSNX-3O5PI-KIQMN-YQPEB-NQTUO-KPPS3-NFYBP-APZSQ"
    private val y2019 = 1_560_000_000_000L
    private val now = 1_790_000_000_000L
    private val codec = GrantCodec()

    @Test fun codesFromTheAdminToolVerifyInTheApp() {
        assertEquals(GrantCheck.Expired, codec.check(unboundExpired, "ANY", now))
        val v = codec.check(unboundExpired, "ANY", y2019)
        assertTrue(v is GrantCheck.Valid)
        assertEquals(424242L, (v as GrantCheck.Valid).grant.id)
        assertEquals(1_577_923_200_000L, v.grant.expiresAtMillis)   // end of 2020-01-01 UTC
    }

    @Test fun phoneBoundCodeWorksOnlyWithItsRequestCode() {
        assertTrue(codec.check(boundExpired, "ABCD-2345", y2019) is GrantCheck.Valid)
        assertTrue(codec.check(boundExpired, "abcd2345", y2019) is GrantCheck.Valid)
        assertEquals(GrantCheck.OtherPhone, codec.check(boundExpired, "ZZZZ2345", y2019))
    }

    @Test fun tamperedOrGarbageCodesAreRejected() {
        val chars = GrantCodec.normalize(unboundExpired).toCharArray()
        chars[10] = if (chars[10] == 'A') 'B' else 'A'                       // change the expiry / id bits
        assertEquals(GrantCheck.BadSignature, codec.check(String(chars), "ANY", y2019))
        assertEquals(GrantCheck.Malformed, codec.check("hello", "ANY", y2019))
        assertEquals(GrantCheck.Malformed, codec.check("", "ANY", y2019))
    }

    @Test fun revokedIdsAreRefused() {
        assertEquals(GrantCheck.Revoked, codec.check(unboundExpired, "ANY", y2019, revoked = setOf(424242L)))
    }

    @Test fun aCodeSignedWithAnyOtherKeyIsRejected() {
        val kp = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val payload = byteArrayOf(1, 0, 0, 0, 7, 0x7f, 0x00, 0, 0, 0, 0)
        val der = Signature.getInstance("SHA256withECDSA").run { initSign(kp.private); update("AURAMIC-PRO-GRANT".toByteArray()); update(payload); sign() }
        val raw = derToRaw(der)
        val forged = GrantCodec.encodeBase32(payload + raw)
        assertEquals(GrantCheck.BadSignature, codec.check(forged, "ANY", y2019))
        // ...but the same code verifies with that key, so the format itself is right
        assertTrue(GrantCodec(kp.public.encoded).check(forged, "ANY", y2019) is GrantCheck.Valid)
    }

    @Test fun grantGivesProAndHighQualityWithoutUsingTrials() {
        val s = ProState(grantUntil = System.currentTimeMillis() + 86_400_000L, trialUsed = 10)
        assertTrue(s.pro); assertTrue(s.highAvailable)
        assertEquals(Quality.HIGH, resolveQuality(null, s))
        val expired = ProState(grantUntil = System.currentTimeMillis() - 1, trialUsed = 10)
        assertFalse(expired.pro)
        assertEquals(Quality.MEDIUM, resolveQuality(Quality.HIGH, expired))
    }

    private fun derToRaw(der: ByteArray): ByteArray {
        var i = 2
        fun next(): ByteArray { i++; val len = der[i++].toInt(); val v = der.copyOfRange(i, i + len); i += len
            val t = v.dropWhile { it.toInt() == 0 }.toByteArray(); return ByteArray(32 - t.size) + t }
        return next() + next()
    }
}
