package com.feedrelay.connections.internal.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 위임 그랜트 애그리거트 — 무엇을 대신할 수 있는가 (§12 재료와 계약).
 * 토큰은 항상 암호문으로만 보관 (§8.5). 사용자당 provider별 1건 (UNIQUE).
 */
@Entity
@Table(name = "connections")
class Connection(
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    val provider: Provider,

    @Column(nullable = false)
    var scopes: String,

    @Column(name = "access_token_enc")
    var accessTokenEnc: String?,

    @Column(name = "refresh_token_enc", nullable = false)
    var refreshTokenEnc: String,

    @Column(name = "expires_at")
    var expiresAt: Instant?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ConnectionStatus = ConnectionStatus.ACTIVE

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

    /** 재동의(re-consent) — 새 그랜트로 교체하고 재활성화 */
    fun replaceGrant(scopes: String, accessTokenEnc: String?, refreshTokenEnc: String, expiresAt: Instant?) {
        this.scopes = scopes
        this.accessTokenEnc = accessTokenEnc
        this.refreshTokenEnc = refreshTokenEnc
        this.expiresAt = expiresAt
        this.status = ConnectionStatus.ACTIVE
    }

    /** 갱신 성공 — access(필요 시 회전된 refresh까지) 교체 */
    fun updateTokens(accessTokenEnc: String, expiresAt: Instant?, rotatedRefreshTokenEnc: String?) {
        this.accessTokenEnc = accessTokenEnc
        this.expiresAt = expiresAt
        rotatedRefreshTokenEnc?.let { this.refreshTokenEnc = it }
    }

    /** 철회 감지 — 이후 토큰 제공 불가, ConnectionRevoked 통지 대상 (§6.3) */
    fun revoke() {
        this.status = ConnectionStatus.REVOKED
    }

    fun isActive(): Boolean = status == ConnectionStatus.ACTIVE
}

enum class Provider { GOOGLE }

enum class ConnectionStatus { ACTIVE, REVOKED }
