package com.feedrelay.sync.internal.application.service

import com.feedrelay.sync.internal.application.port.out.RecordRunPort
import com.feedrelay.sync.internal.application.port.out.SaveSubscriptionPort
import com.feedrelay.sync.internal.domain.RunLog
import com.feedrelay.sync.internal.domain.Subscription
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Run 종료 기록 — 구독 상태·RunLog·통지 이벤트를 한 트랜잭션으로.
 * 이벤트 발행은 트랜잭션 안에서만 레지스트리에 등록된다 (@ApplicationModuleListener 전제, ADR-0009).
 */
@Service
class RunRecorder(
    private val saveSubscriptionPort: SaveSubscriptionPort,
    private val recordRunPort: RecordRunPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun record(subscription: Subscription, runLog: RunLog, event: Any) {
        saveSubscriptionPort.save(subscription)
        recordRunPort.record(runLog)
        eventPublisher.publishEvent(event)
    }
}
