package com.feedrelay.sync.internal.adapter.out.persistence

import com.feedrelay.sync.internal.application.port.out.LoadSubscriptionPort
import com.feedrelay.sync.internal.application.port.out.LoadSyncMappingPort
import com.feedrelay.sync.internal.application.port.out.RecordRunPort
import com.feedrelay.sync.internal.application.port.out.SaveSubscriptionPort
import com.feedrelay.sync.internal.application.port.out.SaveSyncMappingPort
import com.feedrelay.sync.internal.domain.RunLog
import com.feedrelay.sync.internal.domain.Subscription
import com.feedrelay.sync.internal.domain.SyncMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

interface SubscriptionJpaRepository : JpaRepository<Subscription, Long> {
    fun findAllByUserId(userId: Long): List<Subscription>

    fun findAllByConnectionId(connectionId: Long): List<Subscription>
}

interface SyncMappingJpaRepository : JpaRepository<SyncMapping, Long> {
    fun findAllBySubscriptionId(subscriptionId: Long): List<SyncMapping>
}

interface RunLogJpaRepository : JpaRepository<RunLog, Long>

@Component
class SubscriptionPersistenceAdapter(
    private val repository: SubscriptionJpaRepository,
) : LoadSubscriptionPort, SaveSubscriptionPort {

    override fun findById(id: Long): Subscription? = repository.findById(id).orElse(null)

    override fun findAllByUserId(userId: Long): List<Subscription> = repository.findAllByUserId(userId)

    override fun findAllByConnectionId(connectionId: Long): List<Subscription> =
        repository.findAllByConnectionId(connectionId)

    override fun save(subscription: Subscription): Subscription = repository.save(subscription)
}

@Component
class SyncMappingPersistenceAdapter(
    private val repository: SyncMappingJpaRepository,
) : LoadSyncMappingPort, SaveSyncMappingPort {

    override fun findAllBySubscriptionId(subscriptionId: Long): List<SyncMapping> =
        repository.findAllBySubscriptionId(subscriptionId)

    /** 외부 반영 직후 자체 트랜잭션 커밋 — Run 전체 트랜잭션이 없는 이유는 RunSubscriptionService 참조 */
    @Transactional
    override fun save(mapping: SyncMapping): SyncMapping = repository.save(mapping)
}

@Component
class RunLogPersistenceAdapter(
    private val repository: RunLogJpaRepository,
) : RecordRunPort {

    override fun record(runLog: RunLog): RunLog = repository.save(runLog)
}
