package com.feedrelay.ingestion.internal.adapter.out.ical

import com.feedrelay.ingestion.api.IcalSourceConfig
import com.feedrelay.ingestion.api.SourceAdapter
import com.feedrelay.ingestion.api.SourceConfig
import com.feedrelay.ingestion.api.SourceItem
import com.feedrelay.ingestion.api.SourceType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/** ical 프로토콜 수집 어댑터 — Feed(ics 문서)를 받아 SourceItem으로 정규화한다 */
@Component
class IcalSourceAdapter(restClientBuilder: RestClient.Builder) : SourceAdapter {

    private val restClient = restClientBuilder.build()
    private val parser = IcalFeedParser()

    override val type = SourceType.ICAL

    override fun fetch(config: SourceConfig): List<SourceItem> {
        require(config is IcalSourceConfig) { "ICAL 어댑터에 ${config::class.simpleName} 설정이 전달됨" }
        val body = restClient.get().uri(config.url).retrieve().body(String::class.java)
            ?: error("빈 응답: ${config.url}")
        return parser.parse(body)
    }
}
