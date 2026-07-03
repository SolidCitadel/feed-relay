package com.feedrelay.identity.internal.application.port.`in`

/** 세션 주체(google_sub)의 프로필 조회 */
interface CurrentUserQuery {
    fun findByGoogleSub(googleSub: String): UserView?
}

data class UserView(
    val email: String,
    val displayName: String?,
)
