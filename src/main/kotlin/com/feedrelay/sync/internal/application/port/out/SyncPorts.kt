package com.feedrelay.sync.internal.application.port.out

import com.feedrelay.sync.internal.domain.RunLog
import com.feedrelay.sync.internal.domain.Subscription
import com.feedrelay.sync.internal.domain.SyncMapping

interface LoadSubscriptionPort {
    fun findById(id: Long): Subscription?

    fun findAllByUserId(userId: Long): List<Subscription>

    fun findAllByConnectionId(connectionId: Long): List<Subscription>
}

interface SaveSubscriptionPort {
    fun save(subscription: Subscription): Subscription
}

interface LoadSyncMappingPort {
    fun findAllBySubscriptionId(subscriptionId: Long): List<SyncMapping>
}

/** 항목 단위 저장 — 외부 반영 직후 자체 트랜잭션으로 커밋해 중복 생성을 차단한다 */
interface SaveSyncMappingPort {
    fun save(mapping: SyncMapping): SyncMapping
}

interface RecordRunPort {
    fun record(runLog: RunLog): RunLog
}
