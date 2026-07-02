package com.feedrelay.rules

import com.feedrelay.ingestion.api.ItemKind
import com.feedrelay.ingestion.api.SourceItem
import com.feedrelay.rules.api.RuleOutcome
import com.feedrelay.rules.internal.adapter.out.template.ResourceTemplateCatalog
import com.feedrelay.rules.internal.domain.DefaultRuleEngine
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** canvas-v1 템플릿 동작 검증 — 소스 유래 스키마(§8.7) 형태의 SourceItem 기준 */
class CanvasTemplateTests {

    private val engine = DefaultRuleEngine()
    private val canvasV1 = ResourceTemplateCatalog(jacksonObjectMapper()).load("canvas-v1")

    private fun canvasItem(uid: String, summary: String, startAt: Instant? = null) = SourceItem(
        sourceUid = uid,
        kind = ItemKind.EVENT, // Canvas 피드는 전부 VEVENT (§8.7)
        title = summary,
        description = null,
        url = null,
        dueAt = null,
        startAt = startAt,
        endAt = startAt,
        raw = mapOf("uid" to uid, "summary" to summary),
    )

    @Test
    fun `과제는 course_code slot으로 라우팅하고 제목에서 과목 표기를 벗긴다`() {
        val due = Instant.parse("2026-07-10T14:59:00Z")

        val outcome = engine.evaluate(
            canvasItem("event-assignment-101", "과제 3: 이진 힙 구현 [CS201-01]", startAt = due),
            canvasV1,
        )

        val route = assertIs<RuleOutcome.Route>(outcome)
        assertEquals("CS201-01", route.slot)
        assertEquals("과제 3: 이진 힙 구현", route.outbound.title)
        assertEquals(due, route.outbound.dueAt) // DTSTART=마감 → dueAt 강등 (§8.2)
    }

    @Test
    fun `sub-assignment(체크포인트형 과제)도 과제로 라우팅한다`() {
        val outcome = engine.evaluate(
            canvasItem("event-sub-assignment-102", "토론 답글 (필수 답글 2개) [CS201-01]"),
            canvasV1,
        )

        assertEquals("CS201-01", assertIs<RuleOutcome.Route>(outcome).slot)
    }

    @Test
    fun `일정(calendar-event)은 course_code가 붙어 있어도 제외한다`() {
        val outcome = engine.evaluate(
            canvasItem("event-calendar-event-201", "중간 설문 주간 [CS201-01]"),
            canvasV1,
        )

        assertIs<RuleOutcome.Exclude>(outcome)
    }

    @Test
    fun `과목 표기 없는 과제는 _inbox로 - 누락 0 원칙`() {
        val outcome = engine.evaluate(
            canvasItem("event-assignment-104", "과목 표기 없는 과제"),
            canvasV1,
        )

        val route = assertIs<RuleOutcome.Route>(outcome)
        assertEquals("_inbox", route.slot)
        assertEquals("과목 표기 없는 과제", route.outbound.title)
    }
}
