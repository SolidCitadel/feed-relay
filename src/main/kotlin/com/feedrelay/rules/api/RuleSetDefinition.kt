package com.feedrelay.rules.api

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Template과 RuleSet이 공용하는 규칙 정의 값 객체 (§12) — JSON 직렬화 형태(§8.2)와 1:1 대응.
 */
data class RuleSetDefinition(
    val version: Int,
    val rules: List<Rule>,
    val fallback: Action,
)

/** 단일 매치-액션 단위 — 평가는 first-match-wins로 순서에 의존한다 */
data class Rule(
    val id: String,
    val match: Match,
    val action: Action,
)

/**
 * 필드 + 정규식 매칭 조건 (캡처 그룹 포함).
 * field는 raw 키(소문자 ical 프로퍼티: uid·summary·description·…) 또는 정규화 필드(kind·title).
 */
data class Match(
    val field: String,
    val regex: String,
)

/** 매치 시 행위 — route(slot 지정 + Transform) 또는 exclude */
data class Action(
    val type: ActionType,
    /** route 전용 — `${캡처명}` 치환 지원 */
    val slot: String? = null,
    val transform: Transform? = null,
)

enum class ActionType {
    @JsonProperty("route")
    ROUTE,

    @JsonProperty("exclude")
    EXCLUDE,
}

/** 캡처 치환(`${캡처명}`)으로 OutboundItem 필드를 재작성하는 정의 */
data class Transform(
    val title: String? = null,
)
