package com.feedrelay.sync.internal.domain

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
 * (Subscription, sourceUid) → ExternalRef + content_hash + 상태 — 멱등성의 단위 (§12, ADR-0003).
 * 항목 단위 갱신을 위해 독립 애그리거트 (§5).
 */
@Entity
@Table(name = "sync_mappings")
class SyncMapping(
    @Column(name = "subscription_id", nullable = false, updatable = false)
    val subscriptionId: Long,

    @Column(name = "source_uid", nullable = false, updatable = false)
    val sourceUid: String,

    /** 대상 앱 내 항목 위치 — 재라우팅에도 고정 (ADR-0003) */
    @Column(name = "external_ref", nullable = false, updatable = false)
    val externalRef: String,

    @Column(name = "content_hash", nullable = false)
    var contentHash: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MappingStatus = MappingStatus.ACTIVE

    @Enumerated(EnumType.STRING)
    @Column(name = "frozen_reason")
    var frozenReason: FrozenReason? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    fun isFrozen(): Boolean = status == MappingStatus.FROZEN

    /** 이후 불개입 — 사용자 의사 존중 (ADR-0003) */
    fun freeze(reason: FrozenReason) {
        this.status = MappingStatus.FROZEN
        this.frozenReason = reason
        this.updatedAt = Instant.now()
    }

    fun refreshHash(hash: String) {
        this.contentHash = hash
        this.updatedAt = Instant.now()
    }
}

enum class MappingStatus { ACTIVE, FROZEN }

enum class FrozenReason { COMPLETED, DELETED }
