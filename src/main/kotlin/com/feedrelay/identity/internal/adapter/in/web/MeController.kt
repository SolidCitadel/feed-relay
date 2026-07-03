package com.feedrelay.identity.internal.adapter.`in`.web

import com.feedrelay.identity.internal.application.port.`in`.CurrentUserQuery
import com.feedrelay.identity.internal.application.port.`in`.UserView
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MeController(
    private val currentUser: CurrentUserQuery,
) {
    /** 세션 주체의 프로필 — 세션은 있으나 users 행이 없는 이상 상태는 401로 재로그인 유도 */
    @GetMapping("/api/me")
    fun me(@AuthenticationPrincipal principal: OidcUser): ResponseEntity<UserView> {
        val sub = principal.subject ?: return ResponseEntity.status(401).build()
        return currentUser.findByGoogleSub(sub)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(401).build()
    }
}
