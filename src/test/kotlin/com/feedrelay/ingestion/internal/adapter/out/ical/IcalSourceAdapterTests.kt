package com.feedrelay.ingestion.internal.adapter.out.ical

import com.feedrelay.ingestion.api.IcalSourceConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.web.client.RestClient
import kotlin.test.Test
import kotlin.test.assertEquals

class IcalSourceAdapterTests {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name")) { "fixture 없음: $name" }
            .readAllBytes().decodeToString()

    @Test
    fun `HTTP로 Feed를 받아 SourceItem으로 정규화한다`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "text/calendar; charset=utf-8")
                    .setBody(fixture("canvas-feed.ics")),
            )
            server.start()
            val adapter = IcalSourceAdapter(RestClient.builder())

            val items = adapter.fetch(IcalSourceConfig(server.url("/feeds/calendars/user_abc.ics").toString()))

            assertEquals(5, items.size)
            assertEquals("event-assignment-101", items.first().sourceUid)
        }
    }
}
