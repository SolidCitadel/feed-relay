package com.feedrelay.ingestion.internal.adapter.out.ical

import com.feedrelay.ingestion.api.ItemKind
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IcalFeedParserTests {

    private val parser = IcalFeedParser()

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name")) { "fixture 없음: $name" }
            .readAllBytes().decodeToString()

    @Test
    fun `Canvas 피드를 SourceItem으로 정규화한다`() {
        val items = parser.parse(fixture("canvas-feed.ics"))

        assertEquals(5, items.size)
        val assignment = items.first { it.sourceUid == "event-assignment-101" }
        assertEquals(ItemKind.EVENT, assignment.kind) // 컴포넌트 유래 사실 — 과제 판별은 템플릿 소관 (ADR-0012)
        assertEquals("과제 3: 이진 힙 구현 [CS201-01]", assignment.title)
        assertEquals(Instant.parse("2026-07-10T14:59:00Z"), assignment.startAt)
        assertNull(assignment.dueAt)
        assertEquals("배열 기반 이진 힙을 구현한다\n제출물: 소스 코드와 보고서", assignment.description)
        assertEquals("https://canvas.example.edu/courses/11/assignments/101", assignment.url)
        assertEquals("event-assignment-101", assignment.raw["uid"])
        assertTrue(assignment.raw.containsKey("x-alt-desc"))
    }

    @Test
    fun `종일(DATE) 이벤트는 UTC 자정으로 해석하고 DTEND 생략을 수용한다`() {
        val item = parser.parse(fixture("canvas-feed.ics")).first { it.sourceUid == "event-calendar-event-201" }

        assertEquals(Instant.parse("2026-07-15T00:00:00Z"), item.startAt)
        assertNull(item.endAt)
    }

    @Test
    fun `75바이트 접기(folding)를 풀어 원문을 복원한다`() {
        val item = parser.parse(fixture("canvas-feed.ics")).first { it.sourceUid == "event-assignment-103" }

        assertEquals(
            "아주 긴 제목의 과제로 75바이트 접기 규칙이 적용되는 사례를 검증하기 위한 라인 폴딩 테스트 [DS301-02]",
            item.title,
        )
    }

    @Test
    fun `같은 피드를 두 번 파싱하면 동일한 결과 - 멱등성의 기반`() {
        val ics = fixture("canvas-feed.ics")

        assertEquals(parser.parse(ics), parser.parse(ics))
    }

    @Test
    fun `VTODO는 TASK로, DUE는 dueAt으로 정규화한다`() {
        val todo = parser.parse(fixture("generic-todo.ics")).single()

        assertEquals(ItemKind.TASK, todo.kind)
        assertEquals(Instant.parse("2026-07-30T12:00:00Z"), todo.dueAt)
    }
}
