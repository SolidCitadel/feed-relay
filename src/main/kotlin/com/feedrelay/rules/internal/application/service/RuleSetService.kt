package com.feedrelay.rules.internal.application.service

import com.feedrelay.rules.api.RuleSetQuery
import com.feedrelay.rules.api.RuleSetDefinition
import com.feedrelay.rules.api.RuleSetView
import com.feedrelay.rules.internal.adapter.out.template.ResourceTemplateCatalog
import com.feedrelay.rules.internal.application.port.`in`.CreateRuleSetFromTemplateUseCase
import com.feedrelay.rules.internal.application.port.`in`.RuleSetSummary
import com.feedrelay.rules.internal.application.port.`in`.RuleSetsQuery
import com.feedrelay.rules.internal.application.port.`in`.TemplateSummary
import com.feedrelay.rules.internal.application.port.out.LoadRuleSetPort
import com.feedrelay.rules.internal.application.port.out.SaveRuleSetPort
import com.feedrelay.rules.internal.domain.RuleSet
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RuleSetService(
    private val loadRuleSetPort: LoadRuleSetPort,
    private val saveRuleSetPort: SaveRuleSetPort,
    private val templateCatalog: ResourceTemplateCatalog,
) : CreateRuleSetFromTemplateUseCase, RuleSetsQuery, RuleSetQuery {

    @Transactional
    override fun create(userId: Long, templateKey: String): RuleSetSummary {
        val raw = templateCatalog.loadRaw(templateKey)
        val definition = templateCatalog.parse(raw)
        val saved = saveRuleSetPort.save(
            RuleSet(
                userId = userId,
                originTemplateKey = templateKey,
                originTemplateVersion = definition.version,
                definitionJson = raw,
            ),
        )
        return saved.toSummary()
    }

    @Transactional(readOnly = true)
    override fun listFor(userId: Long): List<RuleSetSummary> =
        loadRuleSetPort.findAllByUserId(userId).map { it.toSummary() }

    override fun listTemplates(): List<TemplateSummary> =
        templateCatalog.keys().map { TemplateSummary(key = it, version = templateCatalog.load(it).version) }

    @Transactional(readOnly = true)
    override fun definitionOf(ruleSetId: Long): RuleSetDefinition {
        val ruleSet = checkNotNull(loadRuleSetPort.findById(ruleSetId)) { "RuleSet 없음: $ruleSetId" }
        return templateCatalog.parse(ruleSet.definitionJson)
    }

    @Transactional(readOnly = true)
    override fun findOwned(ruleSetId: Long, userId: Long): RuleSetView? =
        loadRuleSetPort.findById(ruleSetId)
            ?.takeIf { it.userId == userId }
            ?.let { RuleSetView(id = checkNotNull(it.id), originTemplateKey = it.originTemplateKey) }

    private fun RuleSet.toSummary() = RuleSetSummary(
        id = checkNotNull(id),
        originTemplateKey = originTemplateKey,
        originTemplateVersion = originTemplateVersion,
    )
}
