package com.feedrelay.rules.api

import java.time.Instant

/**
 * rules가 산출한, 대상에 쓰려는 **의도** (§12) — 변환 후 값.
 * content_hash와 delivery의 대상은 항상 이 타입이다 — 변환 전 값(SourceItem)의 해싱·배달을 타입으로 차단한다.
 */
data class OutboundItem(
    val title: String,
    val notes: String?,
    val dueAt: Instant?,
    val url: String?,
)
