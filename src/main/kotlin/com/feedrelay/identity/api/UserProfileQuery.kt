package com.feedrelay.identity.api

import java.time.ZoneId

/** 파이프라인(sync)이 실행 시점에 참조하는 사용자 설정 조회 */
interface UserProfileQuery {
    /** 존재하지 않는 사용자면 예외 — 호출자는 FK로 존재가 보장된 id만 전달한다 */
    fun timezoneOf(userId: Long): ZoneId
}
