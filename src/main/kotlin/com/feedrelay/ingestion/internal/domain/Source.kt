package com.feedrelay.ingestion.internal.domain

import com.feedrelay.ingestion.api.SourceType
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
 * 사용자가 등록한 수집처 엔티티 (§12 수집 계열) — config_json에 프로토콜별 수집 설정.
 */
@Entity
@Table(name = "sources")
class Source(
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    val type: SourceType,

    @Column(nullable = false)
    var name: String,

    @Column(name = "config_json", nullable = false)
    var configJson: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SourceStatus = SourceStatus.ACTIVE

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
}

enum class SourceStatus { ACTIVE }
