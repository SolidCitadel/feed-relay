package com.feedrelay.rules.internal.application.port.out

import com.feedrelay.rules.internal.domain.RuleSet

interface LoadRuleSetPort {
    fun findById(id: Long): RuleSet?

    fun findAllByUserId(userId: Long): List<RuleSet>
}

interface SaveRuleSetPort {
    fun save(ruleSet: RuleSet): RuleSet
}
