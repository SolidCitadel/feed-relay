package com.feedrelay.ingestion.internal.adapter.`in`.web

import com.feedrelay.identity.api.AuthenticatedUser
import com.feedrelay.ingestion.internal.application.port.`in`.RegisterSourceCommand
import com.feedrelay.ingestion.internal.application.port.`in`.RegisterSourceUseCase
import com.feedrelay.ingestion.internal.application.port.`in`.RegisteredSource
import com.feedrelay.ingestion.internal.application.port.`in`.SourceSummary
import com.feedrelay.ingestion.internal.application.port.`in`.SourcesQuery
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClientException

@RestController
class SourceController(
    private val registerSource: RegisterSourceUseCase,
    private val sourcesQuery: SourcesQuery,
) {
    /** 등록 = 검증 + 미리보기 — Feed 수집 실패는 422 (URL·형식 문제를 사용자에게 되돌림) */
    @PostMapping("/api/sources")
    fun register(
        @AuthenticationPrincipal principal: Any,
        @RequestBody request: RegisterSourceRequest,
    ): ResponseEntity<*> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        if (request.name.isBlank() || request.url.isBlank()) {
            return ResponseEntity.badRequest().body(ErrorResponse("name과 url은 필수"))
        }
        val registered: RegisteredSource = try {
            registerSource.register(RegisterSourceCommand(userId = userId, name = request.name.trim(), url = request.url.trim()))
        } catch (e: RestClientException) {
            return ResponseEntity.unprocessableEntity().body(ErrorResponse("Feed 수집 실패: ${e.message?.take(200)}"))
        } catch (e: Exception) {
            return ResponseEntity.unprocessableEntity().body(ErrorResponse("Feed 파싱 실패: ${e.message?.take(200)}"))
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(registered)
    }

    @GetMapping("/api/sources")
    fun list(@AuthenticationPrincipal principal: Any): ResponseEntity<List<SourceSummary>> {
        val userId = (principal as? AuthenticatedUser)?.userId
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(sourcesQuery.listFor(userId))
    }
}

data class RegisterSourceRequest(val name: String, val url: String)

data class ErrorResponse(val message: String)
