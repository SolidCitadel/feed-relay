package com.feedrelay.sync.internal.adapter.`in`.web

import com.feedrelay.identity.api.AuthenticatedUser
import com.feedrelay.sync.internal.application.port.`in`.CreateSubscriptionCommand
import com.feedrelay.sync.internal.application.port.`in`.CreateSubscriptionUseCase
import com.feedrelay.sync.internal.application.port.`in`.RunSubscriptionUseCase
import com.feedrelay.sync.internal.application.port.`in`.SubscriptionSummary
import com.feedrelay.sync.internal.application.port.`in`.SubscriptionsQuery
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SubscriptionController(
    private val createSubscription: CreateSubscriptionUseCase,
    private val subscriptionsQuery: SubscriptionsQuery,
    private val runSubscription: RunSubscriptionUseCase,
) {
    @PostMapping("/api/subscriptions")
    fun create(
        @AuthenticationPrincipal principal: Any,
        @RequestBody request: CreateSubscriptionRequest,
    ): ResponseEntity<*> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        val created = try {
            createSubscription.create(
                CreateSubscriptionCommand(
                    userId = userId,
                    sourceId = request.sourceId,
                    ruleSetId = request.ruleSetId,
                    slotMapping = request.slotMapping,
                ),
            )
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/api/subscriptions")
    fun list(@AuthenticationPrincipal principal: Any): ResponseEntity<List<SubscriptionSummary>> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(subscriptionsQuery.listFor(userId))
    }

    /** 수동 트리거 (M3) — 동기 실행 후 결과 요약 반환 (§6.2 "47개 중 32개 등록") */
    @PostMapping("/api/subscriptions/{id}/run")
    fun run(
        @AuthenticationPrincipal principal: Any,
        @PathVariable id: Long,
    ): ResponseEntity<*> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        val summary = try {
            runSubscription.run(id, userId)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))
        }
        return ResponseEntity.ok(summary)
    }
}

data class CreateSubscriptionRequest(
    val sourceId: Long,
    val ruleSetId: Long,
    val slotMapping: Map<String, String>,
)
