package com.feedrelay.sync.internal.application.port.`in`

/** 슬롯 매핑 준비 — 대상 위치 목록·생성 (§6.2). 위임 토큰 해석이 필요해 sync가 조립한다 */
interface TargetsUseCase {
    fun list(userId: Long): List<TargetSummary>

    fun create(userId: Long, name: String): TargetSummary
}

data class TargetSummary(val ref: String, val name: String)
