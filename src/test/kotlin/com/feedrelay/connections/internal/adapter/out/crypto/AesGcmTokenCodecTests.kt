package com.feedrelay.connections.internal.adapter.out.crypto

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AesGcmTokenCodecTests {

    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    private val codec = AesGcmTokenCodec(key)

    @Test
    fun `암복호화 왕복은 원문을 복원한다`() {
        val plain = "1//refresh-token-예시-값"

        assertEquals(plain, codec.decrypt(codec.encrypt(plain)))
    }

    @Test
    fun `같은 평문도 IV가 달라 암호문이 매번 다르다`() {
        assertNotEquals(codec.encrypt("같은 값"), codec.encrypt("같은 값"))
    }

    @Test
    fun `키 미설정이면 사용 시점에 실패한다`() {
        val keyless = AesGcmTokenCodec("")

        assertFailsWith<IllegalStateException> { keyless.encrypt("x") }
    }

    @Test
    fun `키 길이가 32바이트가 아니면 생성 시점에 거부한다`() {
        val shortKey = Base64.getEncoder().encodeToString(ByteArray(16))

        assertFailsWith<IllegalArgumentException> { AesGcmTokenCodec(shortKey) }
    }
}
