package com.feedrelay.ingestion.internal.adapter.out.persistence

import com.feedrelay.ingestion.internal.application.port.out.LoadSourcePort
import com.feedrelay.ingestion.internal.application.port.out.SaveSourcePort
import com.feedrelay.ingestion.internal.domain.Source
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface SourceJpaRepository : JpaRepository<Source, Long> {
    fun findAllByUserId(userId: Long): List<Source>
}

@Component
class SourcePersistenceAdapter(
    private val repository: SourceJpaRepository,
) : LoadSourcePort, SaveSourcePort {

    override fun findById(id: Long): Source? = repository.findById(id).orElse(null)

    override fun findAllByUserId(userId: Long): List<Source> = repository.findAllByUserId(userId)

    override fun save(source: Source): Source = repository.save(source)
}
