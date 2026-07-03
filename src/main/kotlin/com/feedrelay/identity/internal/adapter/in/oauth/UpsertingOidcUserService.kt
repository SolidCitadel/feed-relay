package com.feedrelay.identity.internal.adapter.`in`.oauth

import com.feedrelay.identity.internal.application.port.`in`.UpsertUserCommand
import com.feedrelay.identity.internal.application.port.`in`.UpsertUserUseCase
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

/**
 * OIDC 로그인 성공 시 users를 upsert하는 인바운드 어댑터 (§6.2 가입).
 * SecurityConfig(루트)는 프레임워크 타입(OAuth2UserService)으로만 주입받는다 — 모듈 내부 비노출.
 */
@Component
class UpsertingOidcUserService(
    private val upsertUser: UpsertUserUseCase,
) : OAuth2UserService<OidcUserRequest, OidcUser> {

    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = delegate.loadUser(userRequest)
        upsertUser.upsert(
            UpsertUserCommand(
                googleSub = requireNotNull(oidcUser.subject) { "sub 클레임 없음 — OIDC 필수 클레임" },
                email = requireNotNull(oidcUser.email) { "email 클레임 없음 — email 스코프 필수" },
                displayName = oidcUser.fullName,
            ),
        )
        return oidcUser
    }
}
