package com.feedrelay.connections.api

/** 실행 시점에 유효한 access token을 제공 — 만료 시 갱신, 철회 감지 시 ConnectionRevoked 발행 후 예외 */
interface AccessTokenProvider {
    @Throws(ConnectionRevokedException::class)
    fun accessTokenFor(connectionId: Long): String
}

/** 구독 생성 시 위임 존재·소유 검증용 조회 */
interface ConnectionQuery {
    fun findActiveGoogle(userId: Long): ConnectionView?
}

data class ConnectionView(val id: Long, val userId: Long)

/** 위임이 철회된 상태에서의 토큰 요청 — sync는 해당 구독을 ERROR 처리한다 (§6.3) */
class ConnectionRevokedException(val connectionId: Long) :
    RuntimeException("위임 철회됨: connection=$connectionId")

/** 위임 철회 감지 통지 (커밋 후 비동기, at-least-once — ADR-0006) */
data class ConnectionRevoked(val connectionId: Long, val userId: Long)
