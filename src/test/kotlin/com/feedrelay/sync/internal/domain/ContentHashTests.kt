package com.feedrelay.sync.internal.domain

import com.feedrelay.rules.api.OutboundItem
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ContentHashTests {

    private val item = OutboundItem(
        title = "과제 1",
        notes = "설명",
        dueAt = Instant.parse("2026-07-10T14:59:00Z"),
        url = "https://example.edu/1",
    )

    @Test
    fun `같은 입력은 항상 같은 해시 - 결정론`() {
        assertEquals(ContentHash.of(item, "list-1"), ContentHash.of(item, "list-1"))
    }

    @Test
    fun `필드 경계가 구분된다 - 이어붙임 충돌 없음`() {
        // length-prefix가 없으면 (title="ab", notes="c")와 (title="a", notes="bc")가 같아질 수 있다
        val a = OutboundItem(title = "ab", notes = "c", dueAt = null, url = null)
        val b = OutboundItem(title = "a", notes = "bc", dueAt = null, url = null)

        assertNotEquals(ContentHash.of(a, "t"), ContentHash.of(b, "t"))
    }

    @Test
    fun `null과 빈 문자열이 구분된다`() {
        val withNull = OutboundItem(title = "t", notes = null, dueAt = null, url = null)
        val withEmpty = OutboundItem(title = "t", notes = "", dueAt = null, url = null)

        assertNotEquals(ContentHash.of(withNull, "t"), ContentHash.of(withEmpty, "t"))
    }

    @Test
    fun `시각만 바뀌어도 해시가 바뀐다 - notes의 시각 표기 갱신 유발`() {
        val shifted = item.copy(dueAt = Instant.parse("2026-07-10T15:59:00Z"))

        assertNotEquals(ContentHash.of(item, "list-1"), ContentHash.of(shifted, "list-1"))
    }

    @Test
    fun `TargetRef가 다르면 해시가 다르다`() {
        assertNotEquals(ContentHash.of(item, "list-1"), ContentHash.of(item, "list-2"))
    }
}
