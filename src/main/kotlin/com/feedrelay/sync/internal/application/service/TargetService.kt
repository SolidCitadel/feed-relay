package com.feedrelay.sync.internal.application.service

import com.feedrelay.connections.api.AccessTokenProvider
import com.feedrelay.connections.api.ConnectionQuery
import com.feedrelay.delivery.api.BearerToken
import com.feedrelay.delivery.api.DestinationAdapter
import com.feedrelay.delivery.api.DestinationType
import com.feedrelay.sync.internal.application.port.`in`.TargetSummary
import com.feedrelay.sync.internal.application.port.`in`.TargetsUseCase
import org.springframework.stereotype.Service

@Service
class TargetService(
    private val connectionQuery: ConnectionQuery,
    private val accessTokenProvider: AccessTokenProvider,
    private val adapters: List<DestinationAdapter>,
) : TargetsUseCase {

    override fun list(userId: Long): List<TargetSummary> =
        adapter().listTargets(tokenFor(userId)).map { TargetSummary(ref = it.ref.value, name = it.name) }

    override fun create(userId: Long, name: String): TargetSummary {
        require(name.isNotBlank()) { "리스트 이름은 필수" }
        val ref = adapter().createTarget(tokenFor(userId), name.trim())
        return TargetSummary(ref = ref.value, name = name.trim())
    }

    private fun tokenFor(userId: Long): BearerToken {
        val connection = requireNotNull(connectionQuery.findActiveGoogle(userId)) { "Google Tasks 위임이 필요함" }
        return BearerToken(accessTokenProvider.accessTokenFor(connection.id))
    }

    private fun adapter(): DestinationAdapter =
        checkNotNull(adapters.firstOrNull { it.type == DestinationType.GOOGLE_TASKS }) { "어댑터 없음: GOOGLE_TASKS" }
}
