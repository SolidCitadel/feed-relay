package com.feedrelay.identity.internal.application.port.`in`

import java.time.ZoneId

/** 사용자 타임존 변경 — 유일한 사용자 편집 설정 (프로필은 구글 동기화라 편집 불가) */
interface UpdateTimezoneUseCase {
    fun update(googleSub: String, zone: ZoneId)
}
