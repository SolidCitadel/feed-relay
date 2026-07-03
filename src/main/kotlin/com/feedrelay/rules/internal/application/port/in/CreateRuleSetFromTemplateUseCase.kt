package com.feedrelay.rules.internal.application.port.`in`

/** 템플릿 선택 — 사용자 소유 RuleSet으로 복제 (§6.2, §8.3) */
interface CreateRuleSetFromTemplateUseCase {
    fun create(userId: Long, templateKey: String): RuleSetSummary
}

/** 내 RuleSet·템플릿 목록 (마법사) */
interface RuleSetsQuery {
    fun listFor(userId: Long): List<RuleSetSummary>

    fun listTemplates(): List<TemplateSummary>
}

data class RuleSetSummary(
    val id: Long,
    val originTemplateKey: String,
    val originTemplateVersion: Int,
)

data class TemplateSummary(
    val key: String,
    val version: Int,
)
