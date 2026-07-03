package com.feedrelay.sync.internal.application.service

import com.feedrelay.connections.api.AccessTokenProvider
import com.feedrelay.connections.api.ConnectionRevokedException
import com.feedrelay.delivery.api.BearerToken
import com.feedrelay.delivery.api.DestinationAdapter
import com.feedrelay.delivery.api.DestinationItemState
import com.feedrelay.delivery.api.DestinationSnapshot
import com.feedrelay.delivery.api.DestinationType
import com.feedrelay.delivery.api.ExternalRef
import com.feedrelay.delivery.api.TargetInfo
import com.feedrelay.delivery.api.TargetRef
import com.feedrelay.identity.api.UserProfileQuery
import com.feedrelay.ingestion.api.ItemKind
import com.feedrelay.ingestion.api.SourceItem
import com.feedrelay.ingestion.api.SourceItemsQuery
import com.feedrelay.rules.api.Action
import com.feedrelay.rules.api.ActionType
import com.feedrelay.rules.api.Match
import com.feedrelay.rules.api.OutboundItem
import com.feedrelay.rules.api.Rule
import com.feedrelay.rules.api.RuleSetDefinition
import com.feedrelay.rules.api.RuleSetQuery
import com.feedrelay.rules.api.RuleSetView
import com.feedrelay.rules.api.Transform
import com.feedrelay.rules.internal.domain.DefaultRuleEngine
import com.feedrelay.sync.api.RunFailed
import com.feedrelay.sync.internal.application.port.out.LoadSubscriptionPort
import com.feedrelay.sync.internal.application.port.out.LoadSyncMappingPort
import com.feedrelay.sync.internal.application.port.out.RecordRunPort
import com.feedrelay.sync.internal.application.port.out.SaveSubscriptionPort
import com.feedrelay.sync.internal.application.port.out.SaveSyncMappingPort
import com.feedrelay.sync.internal.domain.FrozenReason
import com.feedrelay.sync.internal.domain.MappingStatus
import com.feedrelay.sync.internal.domain.RunLog
import com.feedrelay.sync.internal.domain.Subscription
import com.feedrelay.sync.internal.domain.SubscriptionStatus
import com.feedrelay.sync.internal.domain.SyncMapping
import org.springframework.context.ApplicationEventPublisher
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.ZoneId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * §10 멱등성·사용자 의사 존중 시나리오를 오케스트레이션 단위에서 고정한다.
 * (전 구간 배선은 시스템 테스트 PipelineSystemTests가 커버)
 */
class RunSubscriptionServiceTests {

    // ── fakes ──────────────────────────────────────────────────────────

    private class InMemorySubscriptions : LoadSubscriptionPort, SaveSubscriptionPort {
        val all = mutableListOf<Subscription>()
        private var nextId = 1L
        override fun findById(id: Long) = all.firstOrNull { it.id == id }
        override fun findAllByUserId(userId: Long) = all.filter { it.userId == userId }
        override fun findAllByConnectionId(connectionId: Long) = all.filter { it.connectionId == connectionId }
        override fun save(subscription: Subscription): Subscription {
            if (subscription.id == null) {
                Subscription::class.java.getDeclaredField("id").apply { isAccessible = true }.set(subscription, nextId++)
                all.add(subscription)
            }
            return subscription
        }
    }

    private class InMemoryMappings : LoadSyncMappingPort, SaveSyncMappingPort {
        val all = mutableListOf<SyncMapping>()
        private var nextId = 1L
        override fun findAllBySubscriptionId(subscriptionId: Long) = all.filter { it.subscriptionId == subscriptionId }
        override fun save(mapping: SyncMapping): SyncMapping {
            if (mapping.id == null) {
                SyncMapping::class.java.getDeclaredField("id").apply { isAccessible = true }.set(mapping, nextId++)
                all.add(mapping)
            }
            return mapping
        }
    }

    /** 인메모리 대상 앱 — 항목 상태를 테스트가 직접 조작(완료·삭제)한다 */
    private class FakeDestination : DestinationAdapter {
        override val type = DestinationType.GOOGLE_TASKS
        data class Stored(var item: OutboundItem, var state: DestinationItemState = DestinationItemState.OPEN)
        val lists = mutableMapOf<String, MutableMap<String, Stored>>()
        private var nextId = 1
        var failCreateForTitle: String? = null
        var createCalls = 0
        var updateCalls = 0

        override fun snapshot(token: BearerToken, target: TargetRef) = DestinationSnapshot(
            lists[target.value].orEmpty().entries.associate { (taskId, stored) ->
                ExternalRef("${target.value}/$taskId") to stored.state
            },
        )

        override fun create(token: BearerToken, target: TargetRef, item: OutboundItem, zone: ZoneId): ExternalRef {
            createCalls++
            check(item.title != failCreateForTitle) { "강제 실패: ${item.title}" }
            val taskId = "t${nextId++}"
            lists.getOrPut(target.value) { mutableMapOf() }[taskId] = Stored(item)
            return ExternalRef("${target.value}/$taskId")
        }

        override fun update(token: BearerToken, ref: ExternalRef, item: OutboundItem, zone: ZoneId) {
            updateCalls++
            val (list, taskId) = ref.value.substringBeforeLast('/') to ref.value.substringAfterLast('/')
            checkNotNull(lists[list]?.get(taskId)) { "없는 항목 update: ${ref.value}" }.item = item
        }

        override fun listTargets(token: BearerToken): List<TargetInfo> = emptyList()
        override fun createTarget(token: BearerToken, name: String) = TargetRef(name)

        fun complete(refValue: String) {
            val (list, taskId) = refValue.substringBeforeLast('/') to refValue.substringAfterLast('/')
            checkNotNull(lists[list]?.get(taskId)).state = DestinationItemState.COMPLETED
        }

        fun delete(refValue: String) {
            val (list, taskId) = refValue.substringBeforeLast('/') to refValue.substringAfterLast('/')
            checkNotNull(lists[list]).remove(taskId)
        }
    }

    // ── 조립 ──────────────────────────────────────────────────────────

    private val subscriptions = InMemorySubscriptions()
    private val mappings = InMemoryMappings()
    private val destination = FakeDestination()
    private val events = mutableListOf<Any>()
    private val runLogs = mutableListOf<RunLog>()
    private var revoked = false
    private var feedItems: List<SourceItem> = emptyList()

    private val definition = RuleSetDefinition(
        version = 1,
        rules = listOf(
            Rule("drop", Match("uid", "^event-calendar-"), Action(ActionType.EXCLUDE)),
            Rule(
                "course",
                Match("summary", """^(?<title>.+?)\s*\[(?<course>[^\[\]]+)]$"""),
                Action(ActionType.ROUTE, slot = "\${course}", transform = Transform(title = "\${title}")),
            ),
        ),
        fallback = Action(ActionType.ROUTE, slot = "_inbox"),
    )

    private val service = RunSubscriptionService(
        loadSubscriptionPort = subscriptions,
        loadSyncMappingPort = mappings,
        saveSyncMappingPort = mappings,
        accessTokenProvider = object : AccessTokenProvider {
            override fun accessTokenFor(connectionId: Long): String {
                if (revoked) throw ConnectionRevokedException(connectionId)
                return "token"
            }
        },
        sourceItemsQuery = object : SourceItemsQuery {
            override fun fetchItems(sourceId: Long) = feedItems
        },
        ruleSetQuery = object : RuleSetQuery {
            override fun definitionOf(ruleSetId: Long) = definition
            override fun findOwned(ruleSetId: Long, userId: Long) = RuleSetView(ruleSetId, "canvas-v1")
        },
        ruleEngine = DefaultRuleEngine(),
        userProfileQuery = object : UserProfileQuery {
            override fun timezoneOf(userId: Long): ZoneId = ZoneId.of("Asia/Seoul")
        },
        adapters = listOf(destination),
        runRecorder = RunRecorder(
            saveSubscriptionPort = subscriptions,
            recordRunPort = object : RecordRunPort {
                override fun record(runLog: RunLog): RunLog = runLog.also { runLogs.add(it) }
            },
            eventPublisher = ApplicationEventPublisher { events.add(it) },
        ),
        objectMapper = jacksonObjectMapper(),
    )

    private lateinit var subscription: Subscription

    @BeforeTest
    fun setUp() {
        subscription = subscriptions.save(
            Subscription(
                userId = 1L,
                sourceId = 10L,
                ruleSetId = 20L,
                connectionId = 30L,
                destinationType = DestinationType.GOOGLE_TASKS,
                slotMappingJson = """{"CS201-01":"list-cs","_inbox":"list-inbox"}""",
            ),
        )
    }

    private fun item(uid: String, summary: String, startAt: Instant? = Instant.parse("2026-07-10T14:59:00Z")) =
        SourceItem(
            sourceUid = uid,
            kind = ItemKind.EVENT,
            title = summary,
            description = null,
            url = null,
            dueAt = null,
            startAt = startAt,
            endAt = startAt,
            raw = mapOf("uid" to uid, "summary" to summary),
        )

    private fun run() = service.run(subscription.id!!, 1L)

    // ── 시나리오 ───────────────────────────────────────────────────────

    @Test
    fun `Q1 - 같은 피드로 2회 연속 실행하면 대상 앱 무변화`() {
        feedItems = listOf(item("a1", "과제 1 [CS201-01]"), item("a2", "과제 2 [CS201-01]"))
        val first = run()
        assertEquals(2, first.created)

        val second = run()

        assertEquals(0, second.created)
        assertEquals(0, second.updated)
        assertEquals(2, second.unchanged)
        assertEquals(2, destination.createCalls)
        assertEquals(0, destination.updateCalls)
    }

    @Test
    fun `Q2 - 마감일 변경 후 실행하면 해당 항목 update 1건만`() {
        feedItems = listOf(item("a1", "과제 1 [CS201-01]"), item("a2", "과제 2 [CS201-01]"))
        run()

        feedItems = listOf(
            item("a1", "과제 1 [CS201-01]", startAt = Instant.parse("2026-07-12T14:59:00Z")),
            item("a2", "과제 2 [CS201-01]"),
        )
        val second = run()

        assertEquals(1, second.updated)
        assertEquals(1, second.unchanged)
        assertEquals(1, destination.updateCalls)
    }

    @Test
    fun `Q3 - 대상에서 완료된 항목은 소스가 바뀌어도 불개입 (FROZEN COMPLETED)`() {
        feedItems = listOf(item("a1", "과제 1 [CS201-01]"))
        run()
        destination.complete(mappings.all.single().externalRef)

        feedItems = listOf(item("a1", "과제 1 [CS201-01]", startAt = Instant.parse("2026-07-15T00:00:00Z")))
        val second = run()
        val third = run()

        assertEquals(FrozenReason.COMPLETED, mappings.all.single().frozenReason)
        assertEquals(1, second.frozen)
        assertEquals(1, third.frozen)
        assertEquals(0, destination.updateCalls)
    }

    @Test
    fun `Q4 - 대상에서 삭제된 항목은 재생성하지 않는다 (FROZEN DELETED)`() {
        feedItems = listOf(item("a1", "과제 1 [CS201-01]"))
        run()
        destination.delete(mappings.all.single().externalRef)

        val second = run()

        assertEquals(MappingStatus.FROZEN, mappings.all.single().status)
        assertEquals(FrozenReason.DELETED, mappings.all.single().frozenReason)
        assertEquals(0, second.created)
        assertEquals(1, destination.createCalls) // 최초 1회뿐
    }

    @Test
    fun `Q8 - 미매핑 slot은 _inbox로 유입 (누락 0)`() {
        feedItems = listOf(item("a1", "과제 1 [MATH-11]")) // MATH-11은 매핑에 없음

        val summary = run()

        assertEquals(1, summary.created)
        assertTrue(mappings.all.single().externalRef.startsWith("list-inbox/"))
    }

    @Test
    fun `일정(exclude 규칙)은 제외되고 나머지는 계속된다`() {
        feedItems = listOf(item("event-calendar-1", "행사 [CS201-01]"), item("a1", "과제 1 [CS201-01]"))

        val summary = run()

        assertEquals(1, summary.excluded)
        assertEquals(1, summary.created)
    }

    @Test
    fun `항목 단위 실패는 격리되고 결과는 PARTIAL - 성공분의 매핑은 보존`() {
        destination.failCreateForTitle = "과제 2"
        feedItems = listOf(item("a1", "과제 1 [CS201-01]"), item("a2", "과제 2 [CS201-01]"))

        val summary = run()

        assertEquals("PARTIAL", summary.result)
        assertEquals(1, summary.created)
        assertEquals(1, summary.failed)
        assertEquals(1, mappings.all.size) // 실패분 매핑 없음 → 다음 실행에서 재시도
    }

    @Test
    fun `위임 철회는 전 구간 실패 - 구독 ERROR + RunFailed 발행`() {
        revoked = true
        feedItems = listOf(item("a1", "과제 1 [CS201-01]"))

        val summary = run()

        assertEquals("FAILED", summary.result)
        assertEquals(SubscriptionStatus.ERROR, subscription.status)
        assertEquals(1, events.filterIsInstance<RunFailed>().size)
    }

    @Test
    fun `수동 재실행 성공은 ERROR를 ACTIVE로 복귀시킨다`() {
        revoked = true
        feedItems = listOf(item("a1", "과제 1 [CS201-01]"))
        run()
        assertEquals(SubscriptionStatus.ERROR, subscription.status)

        revoked = false
        run()

        assertEquals(SubscriptionStatus.ACTIVE, subscription.status)
    }

    @Test
    fun `재라우팅해도 기존 항목은 기존 위치를 유지한다 (ADR-0003)`() {
        feedItems = listOf(item("a1", "과제 1 [CS201-01]"))
        run()
        assertTrue(mappings.all.single().externalRef.startsWith("list-cs/"))

        subscription.slotMappingJson = """{"CS201-01":"list-other","_inbox":"list-inbox"}"""
        val second = run()

        assertEquals(1, second.unchanged) // 내용 동일 + 기존 위치 기준 해시 → 이동도 갱신도 없음
        assertTrue(mappings.all.single().externalRef.startsWith("list-cs/"))
        assertTrue(destination.lists["list-other"].isNullOrEmpty())
    }
}
