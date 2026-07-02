package com.feedrelay.ingestion.api

/**
 * 소스 프로토콜별 수집 어댑터 계약 — 벤더(Canvas 등) 지식은 갖지 않는다 (ADR-0012).
 * 소스 형태별 차이는 템플릿(rules)이 흡수한다.
 */
interface SourceAdapter {
    val type: SourceType

    fun fetch(config: SourceConfig): List<SourceItem>
}

enum class SourceType { ICAL }

/** Source의 config_json이 표현하는 수집 설정 — 프로토콜별로 형태가 다르다 */
sealed interface SourceConfig

data class IcalSourceConfig(val url: String) : SourceConfig
