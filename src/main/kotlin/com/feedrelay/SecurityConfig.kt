package com.feedrelay

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.CsrfTokenRequestHandler
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.security.web.savedrequest.NullRequestCache
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class SecurityConfig {

    /**
     * 위임(google-tasks) 인가 요청에만 offline·재동의·incremental 파라미터를 추가한다 (§8.5).
     * 로그인(google)과 위임이 같은 /oauth2/authorization 진입을 공유하므로 리졸버는 하나로 통일.
     */
    @Bean
    fun authorizationRequestResolver(
        clientRegistrationRepository: ClientRegistrationRepository,
    ): OAuth2AuthorizationRequestResolver {
        val resolver = DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")
        resolver.setAuthorizationRequestCustomizer { builder ->
            builder.attributes { attrs ->
                if (attrs[OAuth2ParameterNames.REGISTRATION_ID] == "google-tasks") {
                    builder.additionalParameters(
                        mapOf(
                            "access_type" to "offline",
                            "prompt" to "consent",
                            "include_granted_scopes" to "true",
                        ),
                    )
                }
            }
        }
        return resolver
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        oidcUserService: OAuth2UserService<OidcUserRequest, OidcUser>,
        authorizationRequestResolver: OAuth2AuthorizationRequestResolver,
    ): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/actuator/health/**", permitAll)
                authorize("/", permitAll)
                authorize("/index.html", permitAll)
                authorize("/assets/**", permitAll)
                authorize("/vite.svg", permitAll)
                authorize("/dashboard", permitAll) // SPA 셸 — 데이터는 /api가 보호
                authorize(anyRequest, authenticated)
            }
            oauth2Login {
                userInfoEndpoint {
                    this.oidcUserService = oidcUserService
                }
                authorizationEndpoint {
                    this.authorizationRequestResolver = authorizationRequestResolver
                }
                // SPA — 로그인 성공은 항상 대시보드로 (saved request 재생 안 함)
                authenticationSuccessHandler = SimpleUrlAuthenticationSuccessHandler("/dashboard")
            }
            oauth2Client {
                // 위임(google-tasks) authorization-code 왕복 — 콜백 처리는 프레임워크,
                // 토큰 저장은 connections의 캡처 저장소 빈(OAuth2AuthorizedClientRepository)이 담당
                authorizationCodeGrant {
                    this.authorizationRequestResolver = authorizationRequestResolver
                }
            }
            requestCache {
                // 보호 대상이 /api/**뿐(내비게이션 경로는 전부 SPA 셸)이라 저장할 요청이 없다
                // — 기본 캐시는 /api/me를 저장해 로그인 후 JSON으로 리다이렉트시킨다
                requestCache = NullRequestCache()
            }
            logout {
                logoutSuccessHandler = HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT) // SPA — 리다이렉트 없이 204
            }
            csrf {
                csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse() // XSRF-TOKEN 쿠키 (§8.5)
                csrfTokenRequestHandler = SpaCsrfTokenRequestHandler()
            }
            exceptionHandling {
                // 미인증 /api/**는 401, 그 외는 구글 로그인으로 (§8.5)
                authenticationEntryPoint = DelegatingAuthenticationEntryPoint.builder()
                    .addEntryPointFor(
                        HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                    )
                    .defaultEntryPoint(LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google"))
                    .build()
            }
            addFilterAfter<BasicAuthenticationFilter>(CsrfCookieFilter())
        }
        return http.build()
    }
}

/**
 * SPA용 CSRF 토큰 핸들러 (Spring Security 공식 SPA 레시피):
 * 헤더(X-XSRF-TOKEN)로 오는 토큰은 쿠키 원문이므로 평문 비교, 그 외는 BREACH 보호(XOR) 유지.
 */
private class SpaCsrfTokenRequestHandler : CsrfTokenRequestHandler {
    private val plain = CsrfTokenRequestAttributeHandler()
    private val xor = XorCsrfTokenRequestAttributeHandler()

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, csrfToken: java.util.function.Supplier<CsrfToken>) =
        xor.handle(request, response, csrfToken)

    override fun resolveCsrfTokenValue(request: HttpServletRequest, csrfToken: CsrfToken): String? =
        if (StringUtils.hasText(request.getHeader(csrfToken.headerName))) {
            plain.resolveCsrfTokenValue(request, csrfToken)
        } else {
            xor.resolveCsrfTokenValue(request, csrfToken)
        }
}

/** 지연 로드되는 CsrfToken을 매 요청 강제 열람해 XSRF-TOKEN 쿠키 발급을 보장한다 */
private class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        (request.getAttribute("_csrf") as? CsrfToken)?.token
        filterChain.doFilter(request, response)
    }
}
