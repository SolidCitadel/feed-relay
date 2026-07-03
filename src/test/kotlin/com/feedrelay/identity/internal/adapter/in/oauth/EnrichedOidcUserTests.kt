package com.feedrelay.identity.internal.adapter.`in`.oauth

import com.feedrelay.identity.api.AuthenticatedUser
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class EnrichedOidcUserTests {

    /** 세션이 Spring Session JDBC(JDK 직렬화)라 principal 직렬화 실패 = 로그인 500 — 회귀 방지 */
    @Test
    fun `principal은 JDK 직렬화 왕복이 가능해야 한다`() {
        val idToken = OidcIdToken.withTokenValue("token")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .subject("google-sub-1")
            .claim("email", "a@example.com")
            .build()
        val principal = EnrichedOidcUser(DefaultOidcUser(emptyList(), idToken), userId = 7L)

        val bytes = ByteArrayOutputStream().also { out ->
            ObjectOutputStream(out).use { it.writeObject(principal) }
        }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }

        assertEquals(7L, (restored as AuthenticatedUser).userId)
        assertEquals("google-sub-1", (restored as OidcUser).subject)
    }
}
