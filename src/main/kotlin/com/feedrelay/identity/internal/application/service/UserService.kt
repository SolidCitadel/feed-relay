package com.feedrelay.identity.internal.application.service

import com.feedrelay.identity.api.UserProfileQuery
import com.feedrelay.identity.internal.application.port.`in`.CurrentUserQuery
import com.feedrelay.identity.internal.application.port.`in`.UpdateTimezoneUseCase
import com.feedrelay.identity.internal.application.port.`in`.UpsertUserCommand
import com.feedrelay.identity.internal.application.port.`in`.UpsertUserUseCase
import com.feedrelay.identity.internal.application.port.`in`.UserView
import com.feedrelay.identity.internal.application.port.out.LoadUserPort
import com.feedrelay.identity.internal.application.port.out.SaveUserPort
import com.feedrelay.identity.internal.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId

@Service
class UserService(
    private val loadUserPort: LoadUserPort,
    private val saveUserPort: SaveUserPort,
) : UpsertUserUseCase, CurrentUserQuery, UpdateTimezoneUseCase, UserProfileQuery {

    @Transactional
    override fun upsert(command: UpsertUserCommand): Long {
        val existing = loadUserPort.findByGoogleSub(command.googleSub)
        val saved = if (existing == null) {
            saveUserPort.save(User(googleSub = command.googleSub, email = command.email, displayName = command.displayName))
        } else {
            existing.syncProfile(command.email, command.displayName)
            saveUserPort.save(existing)
        }
        return checkNotNull(saved.id) { "영속화된 사용자에 id 없음" }
    }

    @Transactional(readOnly = true)
    override fun findByGoogleSub(googleSub: String): UserView? =
        loadUserPort.findByGoogleSub(googleSub)
            ?.let { UserView(email = it.email, displayName = it.displayName, timezone = it.timezone) }

    @Transactional
    override fun update(googleSub: String, zone: ZoneId) {
        val user = checkNotNull(loadUserPort.findByGoogleSub(googleSub)) { "사용자 없음: 세션은 있으나 users 행 부재" }
        user.changeTimezone(zone)
        saveUserPort.save(user)
    }

    @Transactional(readOnly = true)
    override fun timezoneOf(userId: Long): ZoneId =
        ZoneId.of(checkNotNull(loadUserPort.findById(userId)) { "사용자 없음: id=$userId" }.timezone)
}
