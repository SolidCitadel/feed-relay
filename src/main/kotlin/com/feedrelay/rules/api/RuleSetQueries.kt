package com.feedrelay.rules.api

/** 파이프라인 2단계 입력 — RuleSet의 규칙 정의 (§6.1, 호출자: sync) */
interface RuleSetQuery {
    fun definitionOf(ruleSetId: Long): RuleSetDefinition

    /** 구독 생성 시 소유 검증용 */
    fun findOwned(ruleSetId: Long, userId: Long): RuleSetView?
}

data class RuleSetView(val id: Long, val originTemplateKey: String)
