package com.feedrelay

import com.feedrelay.identity.internal.application.port.`in`.UpsertUserCommand
import com.feedrelay.identity.internal.application.port.`in`.UpsertUserUseCase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Testcontainers

/** 시스템층(§8.8): 보안 경로 시맨틱 — 미인증 401(§8.5)·세션 JDBC·CSRF·로그아웃 */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityWebTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var upsertUser: UpsertUserUseCase

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `미인증 api 요청은 302가 아니라 401`() {
        mockMvc.get("/api/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `미인증 비-api 보호 경로는 구글 로그인으로 유도`() {
        mockMvc.get("/protected-page").andExpect {
            status { is3xxRedirection() }
            redirectedUrl("/oauth2/authorization/google")
        }
    }

    @Test
    fun `인증된 세션은 me가 프로필을 반환한다`() {
        upsertUser.upsert(UpsertUserCommand("google-sub-1", "me@example.com", "테스터"))

        mockMvc.get("/api/me") {
            with(oidcLogin().idToken { it.subject("google-sub-1") })
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("me@example.com") }
            jsonPath("$.displayName") { value("테스터") }
        }
    }

    @Test
    fun `세션은 있으나 users 행이 없으면 401 - 재로그인 유도`() {
        mockMvc.get("/api/me") {
            with(oidcLogin().idToken { it.subject("no-row-sub") })
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `로그아웃은 CSRF 토큰 없이는 거부된다`() {
        mockMvc.post("/logout") {
            with(oidcLogin())
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `로그아웃은 CSRF 토큰과 함께 204 - SPA 리다이렉트 없음`() {
        mockMvc.post("/logout") {
            with(oidcLogin())
            with(csrf())
        }.andExpect { status { isNoContent() } }
    }

    @Test
    fun `세션 테이블이 프레임워크에 의해 초기화된다 - 재배포 세션 유지의 전제`() {
        jdbcTemplate.queryForObject("select count(*) from spring_session", Long::class.java)
    }
}
