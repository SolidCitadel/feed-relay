package com.feedrelay.identity.internal.adapter.`in`.web

import com.feedrelay.identity.internal.application.port.`in`.CurrentUserQuery
import com.feedrelay.identity.internal.application.port.`in`.UpdateTimezoneUseCase
import com.feedrelay.identity.internal.application.port.`in`.UserView
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.DateTimeException
import java.time.ZoneId

@RestController
class MeController(
    private val currentUser: CurrentUserQuery,
    private val updateTimezone: UpdateTimezoneUseCase,
) {
    /** 세션 주체의 프로필 — 세션은 있으나 users 행이 없는 이상 상태는 401로 재로그인 유도 */
    @GetMapping("/api/me")
    fun me(@AuthenticationPrincipal principal: OidcUser): ResponseEntity<UserView> {
        val sub = principal.subject ?: return ResponseEntity.status(401).build()
        return currentUser.findByGoogleSub(sub)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(401).build()
    }

    @PatchMapping("/api/me")
    fun patch(@AuthenticationPrincipal principal: OidcUser, @RequestBody request: PatchMeRequest): ResponseEntity<UserView> {
        val sub = principal.subject ?: return ResponseEntity.status(401).build()
        val zone = try {
            ZoneId.of(request.timezone)
        } catch (e: DateTimeException) {
            return ResponseEntity.badRequest().build()
        }
        updateTimezone.update(sub, zone)
        return me(principal)
    }
}

data class PatchMeRequest(val timezone: String)
