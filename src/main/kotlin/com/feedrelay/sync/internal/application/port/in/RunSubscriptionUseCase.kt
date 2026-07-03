package com.feedrelay.sync.internal.application.port.`in`

/** 구독 1회 실행 (§6.1 6단계) — M3는 수동 트리거(웹), M4에서 스케줄러 구동자 추가 */
interface RunSubscriptionUseCase {
    fun run(subscriptionId: Long, requesterId: Long): RunSummary
}

data class RunSummary(
    val subscriptionId: Long,
    val result: String,
    val created: Int = 0,
    val updated: Int = 0,
    val unchanged: Int = 0,
    val excluded: Int = 0,
    val frozen: Int = 0,
    val failed: Int = 0,
    val errorSummary: String? = null,
)
