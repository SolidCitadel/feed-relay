package com.feedrelay.connections.internal.adapter.`in`.web

import com.feedrelay.connections.internal.application.port.`in`.ConnectionStatusQuery
import com.feedrelay.connections.internal.application.port.`in`.ConnectionStatusView
import com.feedrelay.identity.api.AuthenticatedUser
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConnectionController(
    private val statusQuery: ConnectionStatusQuery,
) {
    /** 위임 상태 — google 미연동이면 google=null. 연동 진입은 SPA가 /oauth2/authorization/google-tasks로 이동 */
    @GetMapping("/api/connections")
    fun connections(@AuthenticationPrincipal principal: Any): ResponseEntity<ConnectionsResponse> {
        val userId = (principal as? AuthenticatedUser)?.userId ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(ConnectionsResponse(google = statusQuery.googleStatusFor(userId)))
    }
}

data class ConnectionsResponse(val google: ConnectionStatusView?)
