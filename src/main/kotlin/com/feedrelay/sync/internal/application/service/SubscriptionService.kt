package com.feedrelay.sync.internal.application.service

import com.feedrelay.connections.api.ConnectionQuery
import com.feedrelay.delivery.api.DestinationType
import com.feedrelay.ingestion.api.SourceQuery
import com.feedrelay.rules.api.RuleSetQuery
import com.feedrelay.sync.internal.application.port.`in`.CreateSubscriptionCommand
import com.feedrelay.sync.internal.application.port.`in`.CreateSubscriptionUseCase
import com.feedrelay.sync.internal.application.port.`in`.HandleConnectionRevokedUseCase
import com.feedrelay.sync.internal.application.port.`in`.SubscriptionSummary
import com.feedrelay.sync.internal.application.port.`in`.SubscriptionsQuery
import com.feedrelay.sync.internal.application.port.out.LoadSubscriptionPort
import com.feedrelay.sync.internal.application.port.out.SaveSubscriptionPort
import com.feedrelay.sync.internal.domain.Subscription
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class SubscriptionService(
    private val loadSubscriptionPort: LoadSubscriptionPort,
    private val saveSubscriptionPort: SaveSubscriptionPort,
    private val sourceQuery: SourceQuery,
    private val ruleSetQuery: RuleSetQuery,
    private val connectionQuery: ConnectionQuery,
    private val objectMapper: ObjectMapper,
) : CreateSubscriptionUseCase, SubscriptionsQuery, HandleConnectionRevokedUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun create(command: CreateSubscriptionCommand): SubscriptionSummary {
        requireNotNull(sourceQuery.findOwned(command.sourceId, command.userId)) { "소스 없음 또는 소유 아님" }
        requireNotNull(ruleSetQuery.findOwned(command.ruleSetId, command.userId)) { "RuleSet 없음 또는 소유 아님" }
        val connection = requireNotNull(connectionQuery.findActiveGoogle(command.userId)) { "Google Tasks 위임이 필요함" }
        require(command.slotMapping.containsKey(Subscription.INBOX_SLOT)) { "${Subscription.INBOX_SLOT} 매핑은 필수 (누락 0 원칙)" }
        require(command.slotMapping.values.all { it.isNotBlank() }) { "빈 TargetRef 매핑 존재" }

        val saved = saveSubscriptionPort.save(
            Subscription(
                userId = command.userId,
                sourceId = command.sourceId,
                ruleSetId = command.ruleSetId,
                connectionId = connection.id,
                destinationType = DestinationType.GOOGLE_TASKS,
                slotMappingJson = objectMapper.writeValueAsString(command.slotMapping),
            ),
        )
        return saved.toSummary()
    }

    @Transactional(readOnly = true)
    override fun listFor(userId: Long): List<SubscriptionSummary> =
        loadSubscriptionPort.findAllByUserId(userId).map { it.toSummary() }

    @Transactional
    override fun handle(connectionId: Long) {
        loadSubscriptionPort.findAllByConnectionId(connectionId).forEach { subscription ->
            subscription.markError()
            saveSubscriptionPort.save(subscription)
            log.warn("위임 철회로 구독 ERROR 전이: subscription={}", subscription.id)
        }
    }

    private fun Subscription.toSummary() = SubscriptionSummary(
        id = checkNotNull(id),
        sourceId = sourceId,
        ruleSetId = ruleSetId,
        status = status.name,
        lastRunAt = lastRunAt,
    )
}
