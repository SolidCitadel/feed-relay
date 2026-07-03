package com.feedrelay.connections.internal.application.service

import com.feedrelay.connections.api.ConnectionRevoked
import com.feedrelay.connections.api.ConnectionRevokedException
import com.feedrelay.connections.internal.application.port.`in`.RegisterConnectionCommand
import com.feedrelay.connections.internal.application.port.out.GoogleTokenClient
import com.feedrelay.connections.internal.application.port.out.LoadConnectionPort
import com.feedrelay.connections.internal.application.port.out.RefreshedToken
import com.feedrelay.connections.internal.application.port.out.SaveConnectionPort
import com.feedrelay.connections.internal.application.port.out.TokenCodec
import com.feedrelay.connections.internal.application.port.out.TokenRefreshDeniedException
import com.feedrelay.connections.internal.domain.Connection
import com.feedrelay.connections.internal.domain.ConnectionStatus
import com.feedrelay.connections.internal.domain.Provider
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionServiceTests {

    private class InMemoryConnectionStore : LoadConnectionPort, SaveConnectionPort {
        val all = mutableListOf<Connection>()
        private var nextId = 1L
        override fun findById(id: Long): Connection? = all.firstOrNull { it.id == id }
        override fun findByUserAndProvider(userId: Long, provider: Provider): Connection? =
            all.firstOrNull { it.userId == userId && it.provider == provider }
        override fun save(connection: Connection): Connection {
            if (connection.id == null) {
                Connection::class.java.getDeclaredField("id").apply { isAccessible = true }.set(connection, nextId++)
                all.add(connection)
            }
            return connection
        }
    }

    /** 가역 fake — 암호문임을 프리픽스로 표식 */
    private class FakeTokenCodec : TokenCodec {
        override fun encrypt(plain: String) = "ENC:$plain"
        override fun decrypt(encrypted: String) = encrypted.removePrefix("ENC:")
    }

    private class FakeGoogleTokenClient : GoogleTokenClient {
        var refreshCalls = 0
        var denyRefresh = false
        override fun refresh(refreshToken: String): RefreshedToken {
            refreshCalls++
            if (denyRefresh) throw TokenRefreshDeniedException("invalid_grant")
            return RefreshedToken(accessToken = "new-access", expiresInSeconds = 3600, rotatedRefreshToken = null)
        }
    }

    private val store = InMemoryConnectionStore()
    private val codec = FakeTokenCodec()
    private val tokenClient = FakeGoogleTokenClient()
    private val events = mutableListOf<Any>()
    private val service = ConnectionService(store, store, codec, tokenClient, ApplicationEventPublisher { events.add(it) })

    private fun register(userId: Long = 1L) = service.register(
        RegisterConnectionCommand(
            userId = userId,
            scopes = setOf("https://www.googleapis.com/auth/tasks"),
            accessToken = "access-1",
            accessTokenExpiresAt = Instant.now().plusSeconds(3600),
            refreshToken = "refresh-1",
        ),
    )

    @Test
    fun `등록은 토큰을 암호문으로만 보관한다`() {
        register()

        val saved = store.all.single()
        assertEquals("ENC:refresh-1", saved.refreshTokenEnc)
        assertEquals("ENC:access-1", saved.accessTokenEnc)
        assertEquals(ConnectionStatus.ACTIVE, saved.status)
    }

    @Test
    fun `재동의는 기존 행을 새 그랜트로 교체하고 재활성화한다`() {
        register()
        store.all.single().revoke()

        register()

        val saved = store.all.single()
        assertEquals(ConnectionStatus.ACTIVE, saved.status)
        assertEquals(1, store.all.size)
    }

    @Test
    fun `유효한 캐시가 있으면 refresh 없이 반환한다`() {
        register()

        val token = service.accessTokenFor(store.all.single().id!!)

        assertEquals("access-1", token)
        assertEquals(0, tokenClient.refreshCalls)
    }

    @Test
    fun `만료 임박이면 refresh하고 갱신 토큰을 저장한다`() {
        register()
        val connection = store.all.single()
        connection.updateTokens("ENC:stale", Instant.now().plusSeconds(10), null) // 60초 여유 미만

        val token = service.accessTokenFor(connection.id!!)

        assertEquals("new-access", token)
        assertEquals(1, tokenClient.refreshCalls)
        assertEquals("ENC:new-access", connection.accessTokenEnc)
    }

    @Test
    fun `invalid_grant는 REVOKED 전이 + ConnectionRevoked 발행 + 예외`() {
        register()
        val connection = store.all.single()
        connection.updateTokens("ENC:stale", Instant.now().minusSeconds(1), null)
        tokenClient.denyRefresh = true

        assertFailsWith<ConnectionRevokedException> { service.accessTokenFor(connection.id!!) }

        assertEquals(ConnectionStatus.REVOKED, connection.status)
        val event = events.filterIsInstance<ConnectionRevoked>().single()
        assertEquals(connection.id, event.connectionId)
        assertEquals(1L, event.userId)
    }

    @Test
    fun `REVOKED 상태의 토큰 요청은 즉시 예외`() {
        register()
        val connection = store.all.single()
        connection.revoke()

        assertFailsWith<ConnectionRevokedException> { service.accessTokenFor(connection.id!!) }
        assertEquals(0, tokenClient.refreshCalls)
    }

    @Test
    fun `findActiveGoogle은 ACTIVE만 반환한다`() {
        register()
        assertTrue(service.findActiveGoogle(1L) != null)

        store.all.single().revoke()
        assertNull(service.findActiveGoogle(1L))
    }
}
