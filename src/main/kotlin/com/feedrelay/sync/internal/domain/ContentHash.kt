package com.feedrelay.sync.internal.domain

import com.feedrelay.rules.api.OutboundItem
import java.security.MessageDigest

/**
 * content_hash = SHA-256(OutboundItem + TargetRef) — 변경 감지 기준 (§8.3, ADR-0003).
 * 직렬화 규약: `v1` 프리픽스 + 필드별 length-prefix(title·notes·dueAt·url·targetRef 순),
 * null은 전용 마커 — 구분자 충돌을 원천 차단해 결정론을 보장한다.
 */
object ContentHash {

    fun of(item: OutboundItem, targetRefValue: String): String {
        val canonical = buildString {
            append("v1")
            for (field in listOf(item.title, item.notes, item.dueAt?.toString(), item.url, targetRefValue)) {
                if (field == null) {
                    append("|-")
                } else {
                    append('|').append(field.length).append(':').append(field)
                }
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
