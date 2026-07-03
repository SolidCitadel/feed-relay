package com.feedrelay.connections.internal.adapter.`in`.oauth

import com.feedrelay.connections.internal.application.port.`in`.RegisterConnectionCommand
import com.feedrelay.connections.internal.application.port.`in`.RegisterConnectionUseCase
import com.feedrelay.identity.api.AuthenticatedUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.stereotype.Component

/**
 * 위임(google-tasks) 콜백의 authorized client를 가로채 Connection으로 저장하는 인바운드 어댑터 (§8.5).
 * 토큰을 프레임워크 저장소(세션)에 남기지 않는다 — 진실 원천은 connections 테이블(암호문).
 * 그 외 registration(google 로그인)은 기본 동작에 위임.
 */
@Component
class ConnectionCapturingAuthorizedClientRepository(
    private val registerConnection: RegisterConnectionUseCase,
) : OAuth2AuthorizedClientRepository {

    private val delegate = HttpSessionOAuth2AuthorizedClientRepository()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun <T : OAuth2AuthorizedClient> loadAuthorizedClient(
        clientRegistrationId: String,
        principal: Authentication,
        request: HttpServletRequest,
    ): T? = delegate.loadAuthorizedClient(clientRegistrationId, principal, request)

    override fun saveAuthorizedClient(
        authorizedClient: OAuth2AuthorizedClient,
        principal: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (authorizedClient.clientRegistration.registrationId != TASKS_REGISTRATION_ID) {
            delegate.saveAuthorizedClient(authorizedClient, principal, request, response)
            return
        }
        val userId = (principal.principal as? AuthenticatedUser)?.userId
        val refreshToken = authorizedClient.refreshToken?.tokenValue
        if (userId == null || refreshToken == null) {
            // refresh 부재: prompt=consent 미적용 등 — 저장하지 않으면 /api/connections에 미연동으로 나타나 재시도 유도
            log.warn("위임 캡처 불가: userId={}, refreshToken={}", userId, if (refreshToken == null) "없음" else "있음")
            return
        }
        registerConnection.register(
            RegisterConnectionCommand(
                userId = userId,
                scopes = authorizedClient.accessToken.scopes,
                accessToken = authorizedClient.accessToken.tokenValue,
                accessTokenExpiresAt = authorizedClient.accessToken.expiresAt,
                refreshToken = refreshToken,
            ),
        )
    }

    override fun removeAuthorizedClient(
        clientRegistrationId: String,
        principal: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) = delegate.removeAuthorizedClient(clientRegistrationId, principal, request, response)

    companion object {
        const val TASKS_REGISTRATION_ID = "google-tasks"
    }
}
