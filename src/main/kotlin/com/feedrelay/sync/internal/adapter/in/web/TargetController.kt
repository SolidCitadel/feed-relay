package com.feedrelay.sync.internal.adapter.`in`.web

import com.feedrelay.identity.api.AuthenticatedUser
import com.feedrelay.sync.internal.application.port.`in`.TargetSummary
import com.feedrelay.sync.internal.application.port.`in`.TargetsUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TargetController(
    private val targets: TargetsUseCase,
) {
    @GetMapping("/api/targets")
    fun list(@AuthenticationPrincipal principal: Any): ResponseEntity<*> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        return try {
            ResponseEntity.ok(targets.list(userId))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))
        }
    }

    @PostMapping("/api/targets")
    fun create(
        @AuthenticationPrincipal principal: Any,
        @RequestBody request: CreateTargetRequest,
    ): ResponseEntity<*> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        val created: TargetSummary = try {
            targets.create(userId, request.name)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}

data class CreateTargetRequest(val name: String)
