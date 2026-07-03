package com.feedrelay.identity.internal.adapter.out.persistence

import com.feedrelay.identity.internal.application.port.out.LoadUserPort
import com.feedrelay.identity.internal.application.port.out.SaveUserPort
import com.feedrelay.identity.internal.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByGoogleSub(googleSub: String): User?
}

@Component
class UserPersistenceAdapter(
    private val repository: UserJpaRepository,
) : LoadUserPort, SaveUserPort {

    override fun findByGoogleSub(googleSub: String): User? = repository.findByGoogleSub(googleSub)

    override fun save(user: User): User = repository.save(user)
}
