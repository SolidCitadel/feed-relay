package com.feedrelay.identity.internal.application.port.out

import com.feedrelay.identity.internal.domain.User

interface SaveUserPort {
    fun save(user: User): User
}
