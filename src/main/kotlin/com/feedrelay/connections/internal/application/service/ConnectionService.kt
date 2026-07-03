package com.feedrelay.connections.internal.application.service

import com.feedrelay.connections.api.AccessTokenProvider
import com.feedrelay.connections.api.ConnectionQuery
import com.feedrelay.connections.api.ConnectionRevoked
import com.feedrelay.connections.api.ConnectionRevokedException
import com.feedrelay.connections.api.ConnectionView
import com.feedrelay.connections.internal.application.port.`in`.ConnectionStatusQuery
import com.feedrelay.connections.internal.application.port.`in`.ConnectionStatusView
import com.feedrelay.connections.internal.application.port.`in`.RegisterConnectionCommand
import com.feedrelay.connections.internal.application.port.`in`.RegisterConnectionUseCase
import com.feedrelay.connections.internal.application.port.out.GoogleTokenClient
import com.feedrelay.connections.internal.application.port.out.LoadConnectionPort
import com.feedrelay.connections.internal.application.port.out.SaveConnectionPort
import com.feedrelay.connections.internal.application.port.out.TokenCodec
import com.feedrelay.connections.internal.application.port.out.TokenRefreshDeniedException
import com.feedrelay.connections.internal.domain.Connection
import com.feedrelay.connections.internal.domain.Provider
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ConnectionService(
    private val loadConnectionPort: LoadConnectionPort,
    private val saveConnectionPort: SaveConnectionPort,
    private val tokenCodec: TokenCodec,
    private val googleTokenClient: GoogleTokenClient,
    private val eventPublisher: ApplicationEventPublisher,
) : RegisterConnectionUseCase, ConnectionStatusQuery, AccessTokenProvider, ConnectionQuery {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun register(command: RegisterConnectionCommand) {
        val scopes = command.scopes.sorted().joinToString(" ")
        val accessEnc = tokenCodec.encrypt(command.accessToken)
        val refreshEnc = tokenCodec.encrypt(command.refreshToken)
        val existing = loadConnectionPort.findByUserAndProvider(command.userId, Provider.GOOGLE)
        if (existing == null) {
            saveConnectionPort.save(
                Connection(
                    userId = command.userId,
                    provider = Provider.GOOGLE,
                    scopes = scopes,
                    accessTokenEnc = accessEnc,
                    refreshTokenEnc = refreshEnc,
                    expiresAt = command.accessTokenExpiresAt,
                ),
            )
        } else {
            existing.replaceGrant(scopes, accessEnc, refreshEnc, command.accessTokenExpiresAt)
            saveConnectionPort.save(existing)
        }
    }

    @Transactional(readOnly = true)
    override fun googleStatusFor(userId: Long): ConnectionStatusView? =
        loadConnectionPort.findByUserAndProvider(userId, Provider.GOOGLE)
            ?.let { ConnectionStatusView(status = it.status.name, scopes = it.scopes) }

    @Transactional(readOnly = true)
    override fun findActiveGoogle(userId: Long): ConnectionView? =
        loadConnectionPort.findByUserAndProvider(userId, Provider.GOOGLE)
            ?.takeIf { it.isActive() }
            ?.let { ConnectionView(id = checkNotNull(it.id), userId = it.userId) }

    /**
     * 유효 access token 제공 — 만료 임박(60초 여유) 시 refresh 갱신.
     * invalid_grant → REVOKED 전이 + ConnectionRevoked 발행(커밋 후 전달) + 예외.
     */
    @Transactional
    override fun accessTokenFor(connectionId: Long): String {
        val connection = checkNotNull(loadConnectionPort.findById(connectionId)) { "위임 없음: $connectionId" }
        if (!connection.isActive()) throw ConnectionRevokedException(connectionId)

        val cached = connection.accessTokenEnc
        val expiresAt = connection.expiresAt
        if (cached != null && expiresAt != null && expiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return tokenCodec.decrypt(cached)
        }

        val refreshed = try {
            googleTokenClient.refresh(tokenCodec.decrypt(connection.refreshTokenEnc))
        } catch (e: TokenRefreshDeniedException) {
            log.warn("위임 철회 감지: connection={} — {}", connectionId, e.message)
            connection.revoke()
            saveConnectionPort.save(connection)
            eventPublisher.publishEvent(ConnectionRevoked(connectionId, connection.userId))
            throw ConnectionRevokedException(connectionId)
        }

        connection.updateTokens(
            accessTokenEnc = tokenCodec.encrypt(refreshed.accessToken),
            expiresAt = refreshed.expiresInSeconds?.let { Instant.now().plusSeconds(it) },
            rotatedRefreshTokenEnc = refreshed.rotatedRefreshToken?.let { tokenCodec.encrypt(it) },
        )
        saveConnectionPort.save(connection)
        return refreshed.accessToken
    }
}
