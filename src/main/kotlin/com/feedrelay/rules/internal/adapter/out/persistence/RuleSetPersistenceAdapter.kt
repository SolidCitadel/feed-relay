package com.feedrelay.rules.internal.adapter.out.persistence

import com.feedrelay.rules.internal.application.port.out.LoadRuleSetPort
import com.feedrelay.rules.internal.application.port.out.SaveRuleSetPort
import com.feedrelay.rules.internal.domain.RuleSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface RuleSetJpaRepository : JpaRepository<RuleSet, Long> {
    fun findAllByUserId(userId: Long): List<RuleSet>
}

@Component
class RuleSetPersistenceAdapter(
    private val repository: RuleSetJpaRepository,
) : LoadRuleSetPort, SaveRuleSetPort {

    override fun findById(id: Long): RuleSet? = repository.findById(id).orElse(null)

    override fun findAllByUserId(userId: Long): List<RuleSet> = repository.findAllByUserId(userId)

    override fun save(ruleSet: RuleSet): RuleSet = repository.save(ruleSet)
}
