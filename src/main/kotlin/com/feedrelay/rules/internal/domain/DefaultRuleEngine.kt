package com.feedrelay.rules.internal.domain

import com.feedrelay.ingestion.api.SourceItem
import com.feedrelay.rules.api.Action
import com.feedrelay.rules.api.ActionType
import com.feedrelay.rules.api.Match
import com.feedrelay.rules.api.OutboundItem
import com.feedrelay.rules.api.RuleEngine
import com.feedrelay.rules.api.RuleOutcome
import com.feedrelay.rules.api.RuleSetDefinition
import org.springframework.stereotype.Component

/**
 * first-match-wins 규칙 평가 (§8.2). JPA 비의존 순수 로직 (§8.4).
 *
 * - route의 OutboundItem 기본 구성: title=transform.title 치환(없으면 원문), notes=description,
 *   dueAt=dueAt ?: startAt(마감 미명시 시 시작 시각으로 자연 강등), url=url.
 * - 해소되지 않는 `${캡처명}`은 원문 그대로 남긴다 — 템플릿 오류를 결과에서 보이게 한다.
 */
@Component
class DefaultRuleEngine : RuleEngine {

    override fun evaluate(item: SourceItem, definition: RuleSetDefinition): RuleOutcome {
        for (rule in definition.rules) {
            val captures = matchOrNull(rule.match, item) ?: continue
            return apply(rule.action, item, captures)
        }
        return apply(definition.fallback, item, emptyMap())
    }

    private fun matchOrNull(match: Match, item: SourceItem): Map<String, String>? {
        val value = fieldValue(match.field, item) ?: return null
        return GuardedRegexMatcher.match(match.regex, value)
    }

    /** field 해석: 정규화 필드(kind·title) 우선, 그 외에는 raw 키 (§8.2) */
    private fun fieldValue(field: String, item: SourceItem): String? = when (field) {
        "kind" -> item.kind.name
        "title" -> item.title
        else -> item.raw[field]
    }

    private fun apply(action: Action, item: SourceItem, captures: Map<String, String>): RuleOutcome =
        when (action.type) {
            ActionType.EXCLUDE -> RuleOutcome.Exclude
            ActionType.ROUTE -> RuleOutcome.Route(
                slot = substitute(requireNotNull(action.slot) { "route 액션에는 slot이 필요하다" }, captures),
                outbound = OutboundItem(
                    title = action.transform?.title?.let { substitute(it, captures) } ?: item.title,
                    notes = item.description,
                    dueAt = item.dueAt ?: item.startAt,
                    url = item.url,
                ),
            )
        }

    private fun substitute(template: String, captures: Map<String, String>): String =
        PLACEHOLDER.replace(template) { match -> captures[match.groupValues[1]] ?: match.value }

    companion object {
        private val PLACEHOLDER = Regex("""\$\{([a-zA-Z][a-zA-Z0-9]*)}""")
    }
}
