package com.feedrelay.ingestion.internal.application.port.`in`

/** 내 소스 목록 (대시보드·마법사) */
interface SourcesQuery {
    fun listFor(userId: Long): List<SourceSummary>
}

data class SourceSummary(
    val id: Long,
    val name: String,
    val type: String,
    val status: String,
)
