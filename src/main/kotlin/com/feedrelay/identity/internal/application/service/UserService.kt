package com.feedrelay.identity.internal.application.service

import com.feedrelay.identity.internal.application.port.`in`.CurrentUserQuery
import com.feedrelay.identity.internal.application.port.`in`.UpsertUserCommand
import com.feedrelay.identity.internal.application.port.`in`.UpsertUserUseCase
import com.feedrelay.identity.internal.application.port.`in`.UserView
import com.feedrelay.identity.internal.application.port.out.LoadUserPort
import com.feedrelay.identity.internal.application.port.out.SaveUserPort
import com.feedrelay.identity.internal.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val loadUserPort: LoadUserPort,
    private val saveUserPort: SaveUserPort,
) : UpsertUserUseCase, CurrentUserQuery {

    @Transactional
    override fun upsert(command: UpsertUserCommand) {
        val existing = loadUserPort.findByGoogleSub(command.googleSub)
        if (existing == null) {
            saveUserPort.save(User(googleSub = command.googleSub, email = command.email, displayName = command.displayName))
        } else {
            existing.syncProfile(command.email, command.displayName)
            saveUserPort.save(existing)
        }
    }

    @Transactional(readOnly = true)
    override fun findByGoogleSub(googleSub: String): UserView? =
        loadUserPort.findByGoogleSub(googleSub)?.let { UserView(email = it.email, displayName = it.displayName) }
}
