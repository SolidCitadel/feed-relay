package com.feedrelay.connections.internal.application.port.out

import com.feedrelay.connections.internal.domain.Connection
import com.feedrelay.connections.internal.domain.Provider

interface LoadConnectionPort {
    fun findById(id: Long): Connection?

    fun findByUserAndProvider(userId: Long, provider: Provider): Connection?
}

interface SaveConnectionPort {
    fun save(connection: Connection): Connection
}

/** 토큰 암복호화 — AES-256-GCM, 키는 환경변수 (§8.5) */
interface TokenCodec {
    fun encrypt(plain: String): String

    fun decrypt(encrypted: String): String
}

/** 인증 서버와의 refresh 교환 */
interface GoogleTokenClient {
    @Throws(TokenRefreshDeniedException::class)
    fun refresh(refreshToken: String): RefreshedToken
}

data class RefreshedToken(
    val accessToken: String,
    val expiresInSeconds: Long?,
    /** Google은 보통 회전하지 않지만, 오면 교체한다 */
    val rotatedRefreshToken: String?,
)

/** invalid_grant — 사용자가 위임을 철회했거나 그랜트가 무효화됨 */
class TokenRefreshDeniedException(message: String) : RuntimeException(message)
