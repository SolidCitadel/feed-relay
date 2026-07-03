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

/** Run 1회의 불변 이력 — append-only 독립 애그리거트 (§5, §12) */
@Entity
@Table(name = "run_logs")
class RunLog(
    @Column(name = "subscription_id", nullable = false, updatable = false)
    val subscriptionId: Long,

    @Column(name = "started_at", nullable = false, updatable = false)
    val startedAt: Instant,

    @Column(name = "finished_at", nullable = false, updatable = false)
    val finishedAt: Instant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    val result: RunResult,

    @Column(name = "stats_json", nullable = false, updatable = false)
    val statsJson: String,

    @Column(name = "error_summary", updatable = false)
    val errorSummary: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}

enum class RunResult { SUCCESS, PARTIAL, FAILED }
