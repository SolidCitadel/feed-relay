package com.feedrelay.rules.api

import com.feedrelay.ingestion.api.SourceItem

/** 규칙 평가 — first-match-wins, 미매치 시 definition.fallback 적용 (§8.2) */
interface RuleEngine {
    fun evaluate(item: SourceItem, definition: RuleSetDefinition): RuleOutcome
}

sealed interface RuleOutcome {
    /** Slot으로 분배 — slot은 캡처 치환 결과, 구체 위치(TargetRef) 연결은 sync의 SlotMapping 소관 */
    data class Route(val slot: String, val outbound: OutboundItem) : RuleOutcome

    data object Exclude : RuleOutcome
}
