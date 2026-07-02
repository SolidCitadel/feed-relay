package com.feedrelay.rules.internal.domain

import com.feedrelay.ingestion.api.ItemKind
import com.feedrelay.ingestion.api.SourceItem
import com.feedrelay.rules.api.Action
import com.feedrelay.rules.api.ActionType
import com.feedrelay.rules.api.Match
import com.feedrelay.rules.api.Rule
import com.feedrelay.rules.api.RuleOutcome
import com.feedrelay.rules.api.RuleSetDefinition
import com.feedrelay.rules.api.Transform
import java.time.Instant
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultRuleEngineTests {

    private val engine = DefaultRuleEngine()

    private fun item(
        title: String = "제목",
        kind: ItemKind = ItemKind.EVENT,
        description: String? = null,
        dueAt: Instant? = null,
        startAt: Instant? = null,
        raw: Map<String, String> = emptyMap(),
    ) = SourceItem(
        sourceUid = "uid-1", kind = kind, title = title, description = description,
        url = null, dueAt = dueAt, startAt = startAt, endAt = null, raw = raw,
    )

    private fun definition(vararg rules: Rule, fallback: Action = route("_inbox")) =
        RuleSetDefinition(version = 1, rules = rules.toList(), fallback = fallback)

    private fun route(slot: String, transform: Transform? = null) =
        Action(type = ActionType.ROUTE, slot = slot, transform = transform)

    private val exclude = Action(type = ActionType.EXCLUDE)

    @Test
    fun `first-match-wins - 앞선 규칙이 이긴다`() {
        val definition = definition(
            Rule("first", Match("title", "제"), exclude),
            Rule("second", Match("title", "제목"), route("나중")),
        )

        assertIs<RuleOutcome.Exclude>(engine.evaluate(item(), definition))
    }

    @Test
    fun `캡처 치환 - slot과 transform_title에 캡처를 대입한다`() {
        val definition = definition(
            Rule(
                "course",
                Match("title", """^(?<title>.+?)\s*\[(?<course>[^\[\]]+)]$"""),
                route("\${course}", Transform(title = "\${title}")),
            ),
        )

        val outcome = engine.evaluate(item(title = "과제 1 [자료구조]"), definition)

        val route = assertIs<RuleOutcome.Route>(outcome)
        assertEquals("자료구조", route.slot)
        assertEquals("과제 1", route.outbound.title)
    }

    @Test
    fun `미매치 시 fallback - 원문 title이 유지된다`() {
        val outcome = engine.evaluate(
            item(title = "아무 규칙에도 안 걸림"),
            definition(Rule("never", Match("title", "^절대매치안됨$"), exclude)),
        )

        val route = assertIs<RuleOutcome.Route>(outcome)
        assertEquals("_inbox", route.slot)
        assertEquals("아무 규칙에도 안 걸림", route.outbound.title)
    }

    @Test
    fun `해소되지 않는 캡처 자리는 원문 그대로 남긴다`() {
        val definition = definition(
            Rule("bad", Match("title", "제목"), route("\${missing}")),
        )

        val route = assertIs<RuleOutcome.Route>(engine.evaluate(item(), definition))
        assertEquals("\${missing}", route.slot)
    }

    @Test
    fun `field 해석 - kind는 정규화 필드, 그 외는 raw 키`() {
        val definition = definition(
            Rule("by-kind", Match("kind", "^TASK$"), route("할일")),
            Rule("by-raw", Match("uid", "^event-"), route("유아이디")),
        )

        assertEquals("할일", assertIs<RuleOutcome.Route>(engine.evaluate(item(kind = ItemKind.TASK), definition)).slot)
        assertEquals(
            "유아이디",
            assertIs<RuleOutcome.Route>(
                engine.evaluate(item(raw = mapOf("uid" to "event-assignment-1")), definition),
            ).slot,
        )
    }

    @Test
    fun `존재하지 않는 필드는 미매치로 취급한다`() {
        val definition = definition(Rule("ghost", Match("location", ".*"), exclude))

        assertIs<RuleOutcome.Route>(engine.evaluate(item(), definition))
    }

    @Test
    fun `dueAt 미명시 시 startAt으로 강등한다`() {
        val startAt = Instant.parse("2026-07-10T14:59:00Z")

        val route = assertIs<RuleOutcome.Route>(
            engine.evaluate(item(startAt = startAt), definition()),
        )

        assertEquals(startAt, route.outbound.dueAt)
    }

    @Test
    fun `Q10 - 백트래킹 폭발 정규식은 타임아웃으로 미매치 처리하고 평가를 계속한다`() {
        val definition = definition(Rule("redos", Match("title", "^(a+)+$"), exclude))
        val evil = item(title = "a".repeat(64) + "b")

        lateinit var outcome: RuleOutcome
        val elapsed = measureTimeMillis { outcome = engine.evaluate(evil, definition) }

        val route = assertIs<RuleOutcome.Route>(outcome)
        assertEquals("_inbox", route.slot)
        assertTrue(elapsed < 3_000, "타임아웃 가드가 동작해야 한다 (실측 ${elapsed}ms)")
    }

    @Test
    fun `입력 길이 상한 초과는 미매치로 처리한다`() {
        val definition = definition(Rule("any", Match("title", ".*"), exclude))

        assertIs<RuleOutcome.Route>(engine.evaluate(item(title = "가".repeat(10_001)), definition))
    }

    @Test
    fun `정규식 문법 오류는 미매치로 처리하고 평가를 계속한다`() {
        val definition = definition(Rule("broken", Match("title", "["), exclude))

        assertIs<RuleOutcome.Route>(engine.evaluate(item(), definition))
    }
}
