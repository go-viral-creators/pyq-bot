package com.kaafisir.auramic.data.pro

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** A free-Pro grant issued by the admin (tools/pro_grants.py). */
data class Grant(val id: Long, val expiresAtMillis: Long, val boundTo: Long)

/** Why a code was accepted or not. */
sealed interface GrantCheck {
    data class Valid(val grant: Grant) : GrantCheck
    data object Malformed : GrantCheck
    data object BadSignature : GrantCheck
    data object Expired : GrantCheck
    data object OtherPhone : GrantCheck
    data object Revoked : GrantCheck
}

/**
 * Free Pro for chosen people without a server and without any bypass in the app:
 *
 *  - a grant code is `payload || ECDSA-P256(payload)` signed with the admin's private key, which never leaves the
 *    admin's computer; the app only has [PUBLIC_KEY], so codes cannot be forged from the app or by editing storage
 *    (the stored code is re-verified every time);
 *  - payload = version (1) | grant id (u32) | expiry day since 1970 (u16, valid to the end of that UTC day) |
 *    bound request code hash (u32, 0 = any phone);
 *  - [RevokedGrants] (compiled in) revokes ids; expiry is checked against the latest time this phone has seen, so
 *    turning the clock back does not extend a grant.
 */
class GrantCodec(publicKeyDer: ByteArray = Base64.getDecoder().decode(PUBLIC_KEY)) {
    private val key: PublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKeyDer))

    fun check(code: String, requestCode: String, nowMillis: Long, revoked: Set<Long> = RevokedGrants.IDS): GrantCheck {
        val raw = decodeBase32(normalize(code)) ?: return GrantCheck.Malformed
        if (raw.size != PAYLOAD + 64 || raw[0].toInt() != 1) return GrantCheck.Malformed
        val payload = raw.copyOfRange(0, PAYLOAD)
        val sig = raw.copyOfRange(PAYLOAD, raw.size)
        val ok = runCatching {
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(key); update(DOMAIN); update(payload); verify(rawToDer(sig))
            }
        }.getOrDefault(false)
        if (!ok) return GrantCheck.BadSignature
        val id = u32(payload, 1)
        val expiryDay = ((payload[5].toInt() and 0xff) shl 8) or (payload[6].toInt() and 0xff)
        val bound = u32(payload, 7)
        val grant = Grant(id, (expiryDay + 1L) * DAY_MS, bound)
        return when {
            id in revoked -> GrantCheck.Revoked
            bound != 0L && bound != bindHash(requestCode) -> GrantCheck.OtherPhone
            nowMillis >= grant.expiresAtMillis -> GrantCheck.Expired
            else -> GrantCheck.Valid(grant)
        }
    }

    companion object {
        /** Public half of the admin key (tools/pro_grants.py genkey). Safe to publish. */
        const val PUBLIC_KEY =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7Ymgha+knh+OJQfSl8jkcK5qQxFHhJ4tLnKPak2bdlA5DrwCrNRWRtpjBOXGl6yHuYQGjDAuGjVF+mlAzL68YA=="
        private val DOMAIN = "AURAMIC-PRO-GRANT".toByteArray()
        private const val PAYLOAD = 11
        const val DAY_MS = 86_400_000L

        /** Same hash as tools/pro_grants.py bind_hash(). */
        fun bindHash(requestCode: String): Long {
            val rc = requestCode.uppercase().filter { it.isLetterOrDigit() }
            val d = MessageDigest.getInstance("SHA-256").digest("auramic-install|$rc".toByteArray())
            val h = u32(d, 0)
            return if (h == 0L) 1L else h
        }

        /** Strips "AURA-", dashes, spaces and line breaks; tolerant of lower case. */
        fun normalize(code: String): String {
            val s = code.uppercase().filter { it.isLetterOrDigit() }
            return if (s.startsWith("AURA")) s.substring(4) else s
        }

        private fun u32(b: ByteArray, at: Int): Long =
            ((b[at].toLong() and 0xff) shl 24) or ((b[at + 1].toLong() and 0xff) shl 16) or
                ((b[at + 2].toLong() and 0xff) shl 8) or (b[at + 3].toLong() and 0xff)

        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

        fun decodeBase32(s: String): ByteArray? {
            if (s.isEmpty()) return null
            val out = java.io.ByteArrayOutputStream()
            var buf = 0; var bits = 0
            for (c in s) {
                val v = ALPHABET.indexOf(c)
                if (v < 0) return null
                buf = (buf shl 5) or v; bits += 5
                if (bits >= 8) { bits -= 8; out.write((buf shr bits) and 0xff) }
            }
            return out.toByteArray()
        }

        fun encodeBase32(b: ByteArray): String {
            val sb = StringBuilder()
            var buf = 0; var bits = 0
            for (x in b) {
                buf = (buf shl 8) or (x.toInt() and 0xff); bits += 8
                while (bits >= 5) { bits -= 5; sb.append(ALPHABET[(buf shr bits) and 31]) }
            }
            if (bits > 0) sb.append(ALPHABET[(buf shl (5 - bits)) and 31])
            return sb.toString()
        }

        /** r||s (32+32 bytes) -> ASN.1 DER SEQUENCE { INTEGER r, INTEGER s } as java.security expects. */
        fun rawToDer(sig: ByteArray): ByteArray {
            fun int(x: ByteArray): ByteArray {
                var i = 0
                while (i < x.size - 1 && x[i].toInt() == 0) i++
                var v = x.copyOfRange(i, x.size)
                if (v[0].toInt() and 0x80 != 0) v = byteArrayOf(0) + v
                return byteArrayOf(0x02, v.size.toByte()) + v
            }
            val body = int(sig.copyOfRange(0, 32)) + int(sig.copyOfRange(32, 64))
            return byteArrayOf(0x30, body.size.toByte()) + body
        }
    }
}
