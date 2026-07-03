package com.feedrelay.ingestion.internal.application.port.out

import com.feedrelay.ingestion.internal.domain.Source

interface LoadSourcePort {
    fun findById(id: Long): Source?

    fun findAllByUserId(userId: Long): List<Source>
}

interface SaveSourcePort {
    fun save(source: Source): Source
}
