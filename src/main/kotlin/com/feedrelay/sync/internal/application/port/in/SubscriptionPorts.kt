package com.feedrelay.sync.internal.application.port.`in`

/** 구독 생성 — 재료(Source·RuleSet·Connection)의 소유·존재 검증 포함 (§6.2 슬롯 매핑 단계) */
interface CreateSubscriptionUseCase {
    fun create(command: CreateSubscriptionCommand): SubscriptionSummary
}

data class CreateSubscriptionCommand(
    val userId: Long,
    val sourceId: Long,
    val ruleSetId: Long,
    /** slot → TargetRef 값 — INBOX_SLOT("_inbox") 매핑 필수 */
    val slotMapping: Map<String, String>,
)

interface SubscriptionsQuery {
    fun listFor(userId: Long): List<SubscriptionSummary>
}

data class SubscriptionSummary(
    val id: Long,
    val sourceId: Long,
    val ruleSetId: Long,
    val status: String,
    val lastRunAt: java.time.Instant?,
)

/** 위임 철회 통지 수신 처리 — 해당 위임의 구독을 ERROR로 (§6.3, 멱등) */
interface HandleConnectionRevokedUseCase {
    fun handle(connectionId: Long)
}
