package com.feedrelay.identity.internal.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.ZoneId

/**
 * 사용자 계정 애그리거트 — 인증(누구인가)의 주체 (§12 인증 계열).
 * 도메인·영속 모델 겸용 (no-mapping 전략, ADR-0007). 테이블 소유: identity/users (§5).
 */
@Entity
@Table(name = "users")
class User(
    @Column(name = "google_sub", nullable = false, unique = true, updatable = false)
    val googleSub: String,

    @Column(nullable = false)
    var email: String,

    @Column(name = "display_name")
    var displayName: String?,
) {
    /** IANA 타임존 id — 날짜 전용 대상 필드 변환 기준 (§8.7). 기본 KST, 변경은 changeTimezone으로 */
    @Column(nullable = false)
    var timezone: String = "Asia/Seoul"

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

    /** 구글 클레임으로 프로필 동기화 — 구글이 진실 원천, 매 로그인 수행 (§8.5). 타임존은 사용자 설정이라 불변 */
    fun syncProfile(email: String, displayName: String?) {
        this.email = email
        this.displayName = displayName
    }

    fun changeTimezone(zone: ZoneId) {
        this.timezone = zone.id
    }
}
