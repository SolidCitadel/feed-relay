package com.feedrelay.sync.internal.domain

import com.feedrelay.delivery.api.DestinationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 구독 애그리거트 루트 (§12) — Source × RuleSet × Connection × 대상의 조합 계약.
 * 주기 실행이라는 흐름은 이 애그리거트만이 만든다. 타 모듈 리소스는 id 참조만 (§8.3).
 */
@Entity
@Table(name = "subscriptions")
class Subscription(
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,

    @Column(name = "source_id", nullable = false, updatable = false)
    val sourceId: Long,

    @Column(name = "rule_set_id", nullable = false, updatable = false)
    val ruleSetId: Long,

    @Column(name = "connection_id", nullable = false, updatable = false)
    val connectionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false, updatable = false)
    val destinationType: DestinationType,

    /** slot → TargetRef 값 대응표(SlotMapping, §12) — INBOX_SLOT 매핑이 필수 (누락 0 원칙) */
    @Column(name = "slot_mapping_json", nullable = false)
    var slotMappingJson: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE

    @Column(name = "next_run_at")
    var nextRunAt: Instant? = null

    @Column(name = "last_run_at")
    var lastRunAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

    /** 실행 완주 — ERROR였다면 복귀 (수동 재실행이 복구 수단) */
    fun recordRun(at: Instant) {
        this.lastRunAt = at
        if (this.status == SubscriptionStatus.ERROR) this.status = SubscriptionStatus.ACTIVE
    }

    /** 전 구간 실패·위임 철회 (§6.3) — 멱등 */
    fun markError() {
        this.status = SubscriptionStatus.ERROR
    }

    companion object {
        /** 미매핑 slot·미매치 항목의 폴백 slot (§8.2) */
        const val INBOX_SLOT = "_inbox"
    }
}

enum class SubscriptionStatus { ACTIVE, PAUSED, ERROR }
