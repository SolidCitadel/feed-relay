package com.feedrelay.identity.internal.adapter.`in`.oauth

import com.feedrelay.identity.api.AuthenticatedUser
import com.feedrelay.identity.internal.application.port.`in`.UpsertUserCommand
import com.feedrelay.identity.internal.application.port.`in`.UpsertUserUseCase
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component
import java.io.Serializable

/**
 * OIDC 로그인 성공 시 users를 upsert하고, principal에 userId를 심는 인바운드 어댑터 (§6.2 가입).
 * SecurityConfig(루트)는 프레임워크 타입(OAuth2UserService)으로만 주입받는다 — 모듈 내부 비노출.
 */
@Component
class UpsertingOidcUserService(
    private val upsertUser: UpsertUserUseCase,
) : OAuth2UserService<OidcUserRequest, OidcUser> {

    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = delegate.loadUser(userRequest)
        val userId = upsertUser.upsert(
            UpsertUserCommand(
                googleSub = requireNotNull(oidcUser.subject) { "sub 클레임 없음 — OIDC 필수 클레임" },
                email = requireNotNull(oidcUser.email) { "email 클레임 없음 — email 스코프 필수" },
                displayName = oidcUser.fullName,
            ),
        )
        return EnrichedOidcUser(oidcUser, userId)
    }
}

/**
 * userId가 강화된 세션 주체 — 각 모듈 웹 어댑터가 AuthenticatedUser로 캐스팅해 사용.
 * 세션이 JDBC 저장(JDK 직렬화)이므로 Serializable 필수 — 회귀 테스트로 고정.
 */
internal class EnrichedOidcUser(
    private val delegate: OidcUser,
    override val userId: Long,
) : OidcUser by delegate, AuthenticatedUser, Serializable
