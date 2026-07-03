package com.feedrelay.ingestion.internal.application.service

import com.feedrelay.ingestion.api.IcalSourceConfig
import com.feedrelay.ingestion.api.SourceAdapter
import com.feedrelay.ingestion.api.SourceItem
import com.feedrelay.ingestion.api.SourceItemsQuery
import com.feedrelay.ingestion.api.SourceQuery
import com.feedrelay.ingestion.api.SourceType
import com.feedrelay.ingestion.api.SourceView
import com.feedrelay.ingestion.internal.application.port.`in`.PreviewItem
import com.feedrelay.ingestion.internal.application.port.`in`.RegisterSourceCommand
import com.feedrelay.ingestion.internal.application.port.`in`.RegisterSourceUseCase
import com.feedrelay.ingestion.internal.application.port.`in`.RegisteredSource
import com.feedrelay.ingestion.internal.application.port.`in`.SourceSummary
import com.feedrelay.ingestion.internal.application.port.`in`.SourcesQuery
import com.feedrelay.ingestion.internal.application.port.out.LoadSourcePort
import com.feedrelay.ingestion.internal.application.port.out.SaveSourcePort
import com.feedrelay.ingestion.internal.domain.Source
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class SourceService(
    private val loadSourcePort: LoadSourcePort,
    private val saveSourcePort: SaveSourcePort,
    private val adapters: List<SourceAdapter>,
    private val objectMapper: ObjectMapper,
) : RegisterSourceUseCase, SourcesQuery, SourceItemsQuery, SourceQuery {

    /** 등록 = 검증 — Feed를 즉시 수집해 실패하면 저장하지 않는다 */
    @Transactional
    override fun register(command: RegisterSourceCommand): RegisteredSource {
        val config = IcalSourceConfig(command.url)
        val items = adapterFor(SourceType.ICAL).fetch(config)
        val source = saveSourcePort.save(
            Source(
                userId = command.userId,
                type = SourceType.ICAL,
                name = command.name,
                configJson = objectMapper.writeValueAsString(IcalConfigJson(command.url)),
            ),
        )
        return RegisteredSource(
            id = checkNotNull(source.id),
            name = source.name,
            itemCount = items.size,
            preview = items.take(PREVIEW_LIMIT).map { PreviewItem(it.title, it.kind, it.startAt) },
        )
    }

    @Transactional(readOnly = true)
    override fun listFor(userId: Long): List<SourceSummary> =
        loadSourcePort.findAllByUserId(userId)
            .map { SourceSummary(id = checkNotNull(it.id), name = it.name, type = it.type.name, status = it.status.name) }

    override fun fetchItems(sourceId: Long): List<SourceItem> {
        val source = checkNotNull(loadSourcePort.findById(sourceId)) { "소스 없음: $sourceId" }
        val config = objectMapper.readValue(source.configJson, IcalConfigJson::class.java)
        return adapterFor(source.type).fetch(IcalSourceConfig(config.url))
    }

    @Transactional(readOnly = true)
    override fun findOwned(sourceId: Long, userId: Long): SourceView? =
        loadSourcePort.findById(sourceId)
            ?.takeIf { it.userId == userId }
            ?.let { SourceView(id = checkNotNull(it.id), name = it.name) }

    private fun adapterFor(type: SourceType): SourceAdapter =
        checkNotNull(adapters.firstOrNull { it.type == type }) { "어댑터 없음: $type" }

    private data class IcalConfigJson(val url: String)

    companion object {
        private const val PREVIEW_LIMIT = 10
    }
}
