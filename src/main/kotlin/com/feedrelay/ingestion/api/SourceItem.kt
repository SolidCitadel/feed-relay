package com.feedrelay.ingestion.api

import java.time.Instant

/**
 * Feed를 정규화한 개별 값 객체 — 소스가 말한 **사실**의 진술 (§12).
 * 실행 중에만 흐르는 값으로 영속되지 않으며, 변환 전 원문을 유지한다.
 */
data class SourceItem(
    /** ical UID — 멱등성의 기준 키 */
    val sourceUid: String,
    /** 컴포넌트 유래 사실 (VEVENT→EVENT, VTODO→TASK) — 과제/일정 판별은 템플릿 규칙 소관 (ADR-0012) */
    val kind: ItemKind,
    val title: String,
    val description: String?,
    val url: String?,
    val dueAt: Instant?,
    val startAt: Instant?,
    val endAt: Instant?,
    /** 정규식 매칭 대상 원본 필드 — 소문자 프로퍼티명 키 (동명 중복 시 첫 값) */
    val raw: Map<String, String>,
)

/** SourceItem의 컴포넌트 유래 종별 사실 — 과제/일정의 의미 판별이 아니다 (§12, ADR-0012) */
enum class ItemKind { EVENT, TASK }
