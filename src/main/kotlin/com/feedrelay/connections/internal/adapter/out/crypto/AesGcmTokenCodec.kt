package com.feedrelay.connections.internal.adapter.out.crypto

import com.feedrelay.connections.internal.application.port.out.TokenCodec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 토큰 암복호화 (§8.5) — 산출 포맷 base64(iv ‖ ct ‖ tag).
 * 키는 .env TOKEN_ENC_KEY(base64 32바이트) — 미설정 시 컨텍스트는 뜨되 사용 시점에 실패한다.
 * 키 회전 미지원은 §11 D4.
 */
@Component
class AesGcmTokenCodec(
    @Value("\${feedrelay.token-enc-key:}") keyBase64: String,
) : TokenCodec {

    private val key: SecretKeySpec? =
        if (keyBase64.isBlank()) {
            null
        } else {
            val bytes = Base64.getDecoder().decode(keyBase64)
            require(bytes.size == 32) { "TOKEN_ENC_KEY는 base64 인코딩된 32바이트여야 함 (현재 ${bytes.size}바이트)" }
            SecretKeySpec(bytes, "AES")
        }

    private val random = SecureRandom()

    override fun encrypt(plain: String): String {
        val iv = ByteArray(IV_LENGTH).also(random::nextBytes)
        val cipher = cipher(Cipher.ENCRYPT_MODE, iv)
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    override fun decrypt(encrypted: String): String {
        val bytes = Base64.getDecoder().decode(encrypted)
        val cipher = cipher(Cipher.DECRYPT_MODE, bytes.copyOfRange(0, IV_LENGTH))
        return String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH), Charsets.UTF_8)
    }

    private fun cipher(mode: Int, iv: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, checkNotNull(key) { "TOKEN_ENC_KEY 미설정 — .env 확인 (.env.example 참조)" }, GCMParameterSpec(TAG_BITS, iv))
        }

    companion object {
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
    }
}
