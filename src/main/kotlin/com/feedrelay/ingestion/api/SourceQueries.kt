package com.feedrelay.ingestion.api

/** 파이프라인 1단계 — Source의 Feed를 수집·정규화해 SourceItem으로 (§6.1, 호출자: sync) */
interface SourceItemsQuery {
    fun fetchItems(sourceId: Long): List<SourceItem>
}

/** 구독 생성 시 소유 검증용 조회 (호출자: sync) */
interface SourceQuery {
    fun findOwned(sourceId: Long, userId: Long): SourceView?
}

data class SourceView(val id: Long, val name: String)
