package com.feedrelay.sync.internal.application.service

import com.feedrelay.connections.api.AccessTokenProvider
import com.feedrelay.delivery.api.BearerToken
import com.feedrelay.delivery.api.DestinationAdapter
import com.feedrelay.delivery.api.DestinationItemState
import com.feedrelay.delivery.api.DestinationSnapshot
import com.feedrelay.delivery.api.ExternalRef
import com.feedrelay.delivery.api.TargetRef
import com.feedrelay.identity.api.UserProfileQuery
import com.feedrelay.ingestion.api.SourceItemsQuery
import com.feedrelay.rules.api.OutboundItem
import com.feedrelay.rules.api.RuleEngine
import com.feedrelay.rules.api.RuleOutcome
import com.feedrelay.rules.api.RuleSetQuery
import com.feedrelay.sync.api.RunCompleted
import com.feedrelay.sync.api.RunFailed
import com.feedrelay.sync.internal.application.port.`in`.RunSubscriptionUseCase
import com.feedrelay.sync.internal.application.port.`in`.RunSummary
import com.feedrelay.sync.internal.application.port.out.LoadSubscriptionPort
import com.feedrelay.sync.internal.application.port.out.LoadSyncMappingPort
import com.feedrelay.sync.internal.application.port.out.SaveSyncMappingPort
import com.feedrelay.sync.internal.domain.ContentHash
import com.feedrelay.sync.internal.domain.FrozenReason
import com.feedrelay.sync.internal.domain.RunLog
import com.feedrelay.sync.internal.domain.RunResult
import com.feedrelay.sync.internal.domain.Subscription
import com.feedrelay.sync.internal.domain.SyncMapping
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.ZoneId

/**
 * 구독 실행 오케스트레이션 (§6.1) — fetch → evaluate → snapshot → diff → apply → record.
 * 의도적으로 전체 트랜잭션 없음: 외부 반영(create) 직후 SyncMapping이 자체 트랜잭션으로
 * 커밋되어야 부분 실패에도 중복 생성이 없다 (멱등성 1순위). 항목 단위 실패는 격리(PARTIAL).
 */
@Service
class RunSubscriptionService(
    private val loadSubscriptionPort: LoadSubscriptionPort,
    private val loadSyncMappingPort: LoadSyncMappingPort,
    private val saveSyncMappingPort: SaveSyncMappingPort,
    private val accessTokenProvider: AccessTokenProvider,
    private val sourceItemsQuery: SourceItemsQuery,
    private val ruleSetQuery: RuleSetQuery,
    private val ruleEngine: RuleEngine,
    private val userProfileQuery: UserProfileQuery,
    private val adapters: List<DestinationAdapter>,
    private val runRecorder: RunRecorder,
    private val objectMapper: ObjectMapper,
) : RunSubscriptionUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(subscriptionId: Long, requesterId: Long): RunSummary {
        val subscription = requireNotNull(loadSubscriptionPort.findById(subscriptionId)) { "구독 없음: $subscriptionId" }
        require(subscription.userId == requesterId) { "구독 소유자가 아님" }
        val startedAt = Instant.now()

        return try {
            val summary = execute(subscription)
            subscription.recordRun(Instant.now())
            runRecorder.record(subscription, runLog(subscription, startedAt, summary), summary.toCompletedEvent())
            summary
        } catch (e: Exception) {
            log.warn("Run 전 구간 실패: subscription={}", subscriptionId, e)
            val reason = e.message?.take(500) ?: e.javaClass.simpleName
            subscription.markError()
            val summary = RunSummary(subscriptionId = subscriptionId, result = RunResult.FAILED.name, errorSummary = reason)
            runRecorder.record(subscription, runLog(subscription, startedAt, summary), RunFailed(subscriptionId, reason))
            summary
        }
    }

    private fun execute(subscription: Subscription): RunSummary {
        val subscriptionId = checkNotNull(subscription.id)
        val token = BearerToken(accessTokenProvider.accessTokenFor(subscription.connectionId))
        val items = sourceItemsQuery.fetchItems(subscription.sourceId)
        val definition = ruleSetQuery.definitionOf(subscription.ruleSetId)
        val zone = userProfileQuery.timezoneOf(subscription.userId)
        val slotMapping: Map<String, String> = objectMapper.readValue(subscription.slotMappingJson, MAP_TYPE)
        val inbox = checkNotNull(slotMapping[Subscription.INBOX_SLOT]) { "${Subscription.INBOX_SLOT} 매핑 없음" }
        val adapter = adapterFor(subscription)
        val mappings = loadSyncMappingPort.findAllBySubscriptionId(subscriptionId).associateBy { it.sourceUid }

        var excluded = 0
        val routed = items.mapNotNull { item ->
            when (val outcome = ruleEngine.evaluate(item, definition)) {
                is RuleOutcome.Route -> RoutedItem(
                    sourceUid = item.sourceUid,
                    outbound = outcome.outbound,
                    target = TargetRef(slotMapping[outcome.slot] ?: inbox), // 미매핑 slot → _inbox (Q8)
                )
                is RuleOutcome.Exclude -> {
                    excluded++
                    null
                }
            }
        }.distinctBy { it.sourceUid } // Feed 내 동일 UID 중복은 첫 항목만

        // 스냅샷: 신규 라우팅 대상 + 기존 매핑의 실제 위치(재라우팅에도 기존 위치 유지 — ADR-0003)
        val snapshotTargets = (routed.map { it.target } + mappings.values.map { TargetRef(tasklistOf(it.externalRef)) }).distinct()
        val snapshots: Map<TargetRef, DestinationSnapshot> = snapshotTargets.associateWith { adapter.snapshot(token, it) }

        var created = 0
        var updated = 0
        var unchanged = 0
        var frozen = 0
        var failed = 0
        for (item in routed) {
            try {
                val mapping = mappings[item.sourceUid]
                when {
                    mapping == null -> {
                        val ref = adapter.create(token, item.target, item.outbound, zone)
                        saveSyncMappingPort.save(
                            SyncMapping(
                                subscriptionId = subscriptionId,
                                sourceUid = item.sourceUid,
                                externalRef = ref.value,
                                contentHash = ContentHash.of(item.outbound, item.target.value),
                            ),
                        )
                        created++
                    }
                    mapping.isFrozen() -> frozen++
                    else -> {
                        val currentTarget = tasklistOf(mapping.externalRef)
                        val state = snapshots[TargetRef(currentTarget)]?.byRef?.get(ExternalRef(mapping.externalRef))
                        when (state) {
                            null -> { // 대상에서 소실 = 사용자 삭제 — 재생성하지 않음 (Q4)
                                mapping.freeze(FrozenReason.DELETED)
                                saveSyncMappingPort.save(mapping)
                                frozen++
                            }
                            DestinationItemState.COMPLETED -> { // 완료 — 이후 불개입 (Q3)
                                mapping.freeze(FrozenReason.COMPLETED)
                                saveSyncMappingPort.save(mapping)
                                frozen++
                            }
                            DestinationItemState.OPEN -> {
                                val hash = ContentHash.of(item.outbound, currentTarget)
                                if (hash != mapping.contentHash) {
                                    adapter.update(token, ExternalRef(mapping.externalRef), item.outbound, zone)
                                    mapping.refreshHash(hash)
                                    saveSyncMappingPort.save(mapping)
                                    updated++
                                } else {
                                    unchanged++ // 무변화 (Q1)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                failed++ // 항목 단위 실패는 기록 후 계속 (§6.1 apply — PARTIAL)
                log.warn("항목 반영 실패: subscription={}, uid={}", subscriptionId, item.sourceUid, e)
            }
        }

        val result = if (failed > 0) RunResult.PARTIAL else RunResult.SUCCESS
        return RunSummary(
            subscriptionId = subscriptionId,
            result = result.name,
            created = created,
            updated = updated,
            unchanged = unchanged,
            excluded = excluded,
            frozen = frozen,
            failed = failed,
        )
    }

    private fun adapterFor(subscription: Subscription): DestinationAdapter =
        checkNotNull(adapters.firstOrNull { it.type == subscription.destinationType }) {
            "어댑터 없음: ${subscription.destinationType}"
        }

    /** ExternalRef 인코딩("{tasklistId}/{taskId}")에서 위치 부분 — 어댑터의 불투명 인코딩에 대한 sync의 유일한 가정 */
    private fun tasklistOf(externalRef: String): String = externalRef.substringBeforeLast('/')

    private fun runLog(subscription: Subscription, startedAt: Instant, summary: RunSummary) = RunLog(
        subscriptionId = checkNotNull(subscription.id),
        startedAt = startedAt,
        finishedAt = Instant.now(),
        result = RunResult.valueOf(summary.result),
        statsJson = objectMapper.writeValueAsString(summary),
        errorSummary = summary.errorSummary,
    )

    private fun RunSummary.toCompletedEvent() = RunCompleted(
        subscriptionId = subscriptionId,
        result = result,
        created = created,
        updated = updated,
        unchanged = unchanged,
        excluded = excluded,
        frozen = frozen,
        failed = failed,
    )

    private data class RoutedItem(
        val sourceUid: String,
        val outbound: OutboundItem,
        val target: TargetRef,
    )

    companion object {
        private val MAP_TYPE = object : tools.jackson.core.type.TypeReference<Map<String, String>>() {}
    }
}
