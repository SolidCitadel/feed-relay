package com.feedrelay.rules.internal.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Template을 복제한 사용자 소유 규칙 묶음 (§12) — 원본 key/version 기록, 업데이트는 신규 선택자에게만 (§1).
 * definition_json이 내용물(RuleSetDefinition 직렬형)의 진실 원천.
 */
@Entity
@Table(name = "rule_sets")
class RuleSet(
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,

    @Column(name = "origin_template_key", nullable = false, updatable = false)
    val originTemplateKey: String,

    @Column(name = "origin_template_version", nullable = false, updatable = false)
    val originTemplateVersion: Int,

    @Column(name = "definition_json", nullable = false)
    var definitionJson: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
}
