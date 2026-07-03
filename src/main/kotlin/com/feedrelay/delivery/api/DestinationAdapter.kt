package com.feedrelay.delivery.api

import com.feedrelay.rules.api.OutboundItem
import java.time.ZoneId

/**
 * 대상 앱 반영 계약 (§8.1) — 무상태, 능력 격차(예: Tasks due 날짜만)는 어댑터가 흡수 (§8.7).
 * zone: 날짜 전용 필드 변환에 쓰는 사용자 타임존.
 */
interface DestinationAdapter {
    val type: DestinationType

    /** 리스트당 1콜 — 완료·소실 감지(FROZEN 판정)의 입력 (§6.1) */
    fun snapshot(token: BearerToken, target: TargetRef): DestinationSnapshot

    fun create(token: BearerToken, target: TargetRef, item: OutboundItem, zone: ZoneId): ExternalRef

    fun update(token: BearerToken, ref: ExternalRef, item: OutboundItem, zone: ZoneId)

    /** 대상 위치 목록·생성 — 슬롯 매핑 준비용 (§6.2) */
    fun listTargets(token: BearerToken): List<TargetInfo>

    fun createTarget(token: BearerToken, name: String): TargetRef
}

enum class DestinationType { GOOGLE_TASKS }

@JvmInline
value class BearerToken(val value: String)

/** 대상 앱 안의 구체 위치 (§12 — 예: Google Tasks tasklistId) */
@JvmInline
value class TargetRef(val value: String)

/** 대상 앱에 생성된 개별 항목의 위치 — 어댑터가 인코딩하는 불투명 값 (§12) */
@JvmInline
value class ExternalRef(val value: String)

data class TargetInfo(val ref: TargetRef, val name: String)

/** 대상 항목의 관측 상태 — 스냅샷에 없으면 소실(사용자 삭제)로 판정 */
data class DestinationSnapshot(val byRef: Map<ExternalRef, DestinationItemState>)

enum class DestinationItemState { OPEN, COMPLETED }
