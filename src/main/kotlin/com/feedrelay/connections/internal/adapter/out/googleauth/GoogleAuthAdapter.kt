package com.feedrelay.connections.internal.adapter.out.googleauth

import com.fasterxml.jackson.annotation.JsonProperty
import com.feedrelay.connections.internal.application.port.out.GoogleTokenClient
import com.feedrelay.connections.internal.application.port.out.RefreshedToken
import com.feedrelay.connections.internal.application.port.out.TokenRefreshDeniedException
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

/** Google 토큰 엔드포인트와의 refresh 교환 — 자격·URI는 ClientRegistration(google-tasks)에서 가져온다 */
@Component
class GoogleAuthAdapter(
    restClientBuilder: RestClient.Builder,
    private val clientRegistrations: ClientRegistrationRepository,
) : GoogleTokenClient {

    private val restClient = restClientBuilder.build()

    override fun refresh(refreshToken: String): RefreshedToken {
        val registration = checkNotNull(clientRegistrations.findByRegistrationId("google-tasks")) {
            "ClientRegistration 없음: google-tasks"
        }
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
            add("client_id", registration.clientId)
            add("client_secret", registration.clientSecret)
        }
        val response = try {
            restClient.post()
                .uri(registration.providerDetails.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse::class.java)
        } catch (e: HttpClientErrorException) {
            // invalid_grant = 철회·무효화 (4xx) — 그 외는 일시 장애로 전파
            if (e.responseBodyAsString.contains("invalid_grant")) {
                throw TokenRefreshDeniedException("invalid_grant: ${e.responseBodyAsString.take(200)}")
            }
            throw e
        }
        checkNotNull(response?.accessToken) { "토큰 응답에 access_token 없음" }
        return RefreshedToken(
            accessToken = response.accessToken,
            expiresInSeconds = response.expiresIn,
            rotatedRefreshToken = response.refreshToken,
        )
    }

    private data class TokenResponse(
        @JsonProperty("access_token") val accessToken: String?,
        @JsonProperty("expires_in") val expiresIn: Long?,
        @JsonProperty("refresh_token") val refreshToken: String?,
    )
}
