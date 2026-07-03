package com.feedrelay.rules.internal.adapter.`in`.web

import com.feedrelay.identity.api.AuthenticatedUser
import com.feedrelay.rules.internal.application.port.`in`.CreateRuleSetFromTemplateUseCase
import com.feedrelay.rules.internal.application.port.`in`.RuleSetSummary
import com.feedrelay.rules.internal.application.port.`in`.RuleSetsQuery
import com.feedrelay.rules.internal.application.port.`in`.TemplateSummary
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RuleSetController(
    private val createRuleSet: CreateRuleSetFromTemplateUseCase,
    private val ruleSetsQuery: RuleSetsQuery,
) {
    @GetMapping("/api/templates")
    fun templates(): List<TemplateSummary> = ruleSetsQuery.listTemplates()

    @PostMapping("/api/rulesets")
    fun create(
        @AuthenticationPrincipal principal: Any,
        @RequestBody request: CreateRuleSetRequest,
    ): ResponseEntity<*> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        val created = try {
            createRuleSet.create(userId, request.templateKey)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 템플릿")))
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/api/rulesets")
    fun list(@AuthenticationPrincipal principal: Any): ResponseEntity<List<RuleSetSummary>> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(ruleSetsQuery.listFor(userId))
    }
}

data class CreateRuleSetRequest(val templateKey: String)
