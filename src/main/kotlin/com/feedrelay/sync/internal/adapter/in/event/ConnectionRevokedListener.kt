package com.feedrelay.sync.internal.adapter.`in`.event

import com.feedrelay.connections.api.ConnectionRevoked
import com.feedrelay.sync.internal.application.port.`in`.HandleConnectionRevokedUseCase
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

/** 통지 평면 수신 (§6.3) — 커밋 후 비동기 at-least-once이므로 처리(ERROR 전이)는 멱등이어야 한다 */
@Component
class ConnectionRevokedListener(
    private val handleConnectionRevoked: HandleConnectionRevokedUseCase,
) {
    @ApplicationModuleListener
    fun on(event: ConnectionRevoked) {
        handleConnectionRevoked.handle(event.connectionId)
    }
}
