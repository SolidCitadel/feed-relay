package com.feedrelay.delivery.internal.adapter.out.googletasks

import com.feedrelay.delivery.api.BearerToken
import com.feedrelay.delivery.api.DestinationItemState
import com.feedrelay.delivery.api.ExternalRef
import com.feedrelay.delivery.api.TargetRef
import com.feedrelay.rules.api.OutboundItem
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoogleTasksAdapterTests {

    private val kst = ZoneId.of("Asia/Seoul")
    private val token = BearerToken("token-1")

    private fun withServer(block: (MockWebServer, GoogleTasksAdapter) -> Unit) {
        MockWebServer().use { server ->
            server.start()
            val adapter = GoogleTasksAdapter(RestClient.builder(), server.url("/tasks/v1").toString())
            block(server, adapter)
        }
    }

    @Test
    fun `snapshot은 페이지를 전부 순회하고 완료 상태를 매핑한다`() {
        withServer { server, adapter ->
            server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json").setBody(
                    """{"items":[{"id":"t1","status":"needsAction"},{"id":"t2","status":"completed"}],"nextPageToken":"p2"}""",
                ),
            )
            server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json").setBody(
                    """{"items":[{"id":"t3","status":"needsAction"}]}""",
                ),
            )

            val snapshot = adapter.snapshot(token, TargetRef("list-1"))

            assertEquals(3, snapshot.byRef.size)
            assertEquals(DestinationItemState.COMPLETED, snapshot.byRef[ExternalRef("list-1/t2")])
            assertEquals(DestinationItemState.OPEN, snapshot.byRef[ExternalRef("list-1/t3")])
            val first = server.takeRequest()
            assertTrue(first.path!!.contains("showCompleted=true"))
            assertTrue(first.path!!.contains("showHidden=true"))
            assertEquals("Bearer token-1", first.getHeader("Authorization"))
            assertTrue(server.takeRequest().path!!.contains("pageToken=p2"))
        }
    }

    @Test
    fun `create는 사용자 타임존으로 due 날짜를 변환하고 notes를 관리 필드로 구성한다`() {
        withServer { server, adapter ->
            server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json").setBody("""{"id":"t9"}"""),
            )
            // 15:00Z = KST 다음날 00:00 — UTC 날짜(7/10)가 아니라 KST 날짜(7/11)여야 한다 (§8.7)
            val item = OutboundItem(
                title = "과제 1",
                notes = "본문 설명",
                dueAt = Instant.parse("2026-07-10T15:00:00Z"),
                url = "https://canvas.example.edu/1",
            )

            val ref = adapter.create(token, TargetRef("list-1"), item, kst)

            assertEquals("list-1/t9", ref.value)
            val request = server.takeRequest()
            assertEquals("/tasks/v1/lists/list-1/tasks", request.path)
            val body = request.body.readUtf8()
            assertTrue(body.contains(""""due":"2026-07-11T00:00:00.000Z""""), "KST 날짜로 변환돼야 함: $body")
            assertTrue(body.contains("마감: 2026-07-11 00:00"))
            assertTrue(body.contains("원본: https://canvas.example.edu/1"))
            assertTrue(body.contains("본문 설명"))
        }
    }

    @Test
    fun `update는 ExternalRef 인코딩을 풀어 PATCH한다`() {
        withServer { server, adapter ->
            server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json").setBody("""{"id":"t9"}"""),
            )
            val item = OutboundItem(title = "과제 1", notes = null, dueAt = null, url = null)

            adapter.update(token, ExternalRef("list-1/t9"), item, kst)

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/tasks/v1/lists/list-1/tasks/t9", request.path)
        }
    }

    @Test
    fun `listTargets와 createTarget은 tasklists API를 쓴다`() {
        withServer { server, adapter ->
            server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json").setBody(
                    """{"items":[{"id":"l1","title":"내 목록"}]}""",
                ),
            )
            server.enqueue(
                MockResponse().setHeader("Content-Type", "application/json").setBody("""{"id":"l2","title":"자료구조"}"""),
            )

            val targets = adapter.listTargets(token)
            val created = adapter.createTarget(token, "자료구조")

            assertEquals("l1", targets.single().ref.value)
            assertEquals("내 목록", targets.single().name)
            assertEquals("l2", created.value)
            assertEquals("/tasks/v1/users/@me/lists", server.takeRequest().path)
            assertEquals("POST", server.takeRequest().method)
        }
    }
}
