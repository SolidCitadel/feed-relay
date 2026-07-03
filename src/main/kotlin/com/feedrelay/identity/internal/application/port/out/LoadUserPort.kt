package com.feedrelay.identity.internal.application.port.out

import com.feedrelay.identity.internal.domain.User

interface LoadUserPort {
    fun findByGoogleSub(googleSub: String): User?
}
