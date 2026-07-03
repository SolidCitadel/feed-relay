package com.feedrelay.connections.internal.adapter.out.persistence

import com.feedrelay.connections.internal.application.port.out.LoadConnectionPort
import com.feedrelay.connections.internal.application.port.out.SaveConnectionPort
import com.feedrelay.connections.internal.domain.Connection
import com.feedrelay.connections.internal.domain.Provider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface ConnectionJpaRepository : JpaRepository<Connection, Long> {
    fun findByUserIdAndProvider(userId: Long, provider: Provider): Connection?
}

@Component
class ConnectionPersistenceAdapter(
    private val repository: ConnectionJpaRepository,
) : LoadConnectionPort, SaveConnectionPort {

    override fun findById(id: Long): Connection? = repository.findById(id).orElse(null)

    override fun findByUserAndProvider(userId: Long, provider: Provider): Connection? =
        repository.findByUserIdAndProvider(userId, provider)

    override fun save(connection: Connection): Connection = repository.save(connection)
}
