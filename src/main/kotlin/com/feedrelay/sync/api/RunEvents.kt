package com.feedrelay.sync.api

/** Run 종료 사실 통지 (커밋 후 비동기, at-least-once — ADR-0006). 구독처: notifications(M5) */
data class RunCompleted(
    val subscriptionId: Long,
    val result: String,
    val created: Int,
    val updated: Int,
    val unchanged: Int,
    val excluded: Int,
    val frozen: Int,
    val failed: Int,
)

data class RunFailed(
    val subscriptionId: Long,
    val reason: String,
)
