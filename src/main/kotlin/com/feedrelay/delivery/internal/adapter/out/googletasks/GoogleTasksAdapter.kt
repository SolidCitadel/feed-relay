package com.feedrelay.delivery.internal.adapter.out.googletasks

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.feedrelay.delivery.api.BearerToken
import com.feedrelay.delivery.api.DestinationAdapter
import com.feedrelay.delivery.api.DestinationItemState
import com.feedrelay.delivery.api.DestinationSnapshot
import com.feedrelay.delivery.api.DestinationType
import com.feedrelay.delivery.api.ExternalRef
import com.feedrelay.delivery.api.TargetInfo
import com.feedrelay.delivery.api.TargetRef
import com.feedrelay.rules.api.OutboundItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Google Tasks 어댑터 — ExternalRef = "{tasklistId}/{taskId}" (불투명 인코딩).
 * 능력 격차 흡수 (§8.7): due는 날짜만 반영되므로 사용자 타임존으로 날짜 변환하고,
 * 시각·원본 링크는 notes(관리 필드 — §1)에 보존한다.
 */
@Component
class GoogleTasksAdapter(
    restClientBuilder: RestClient.Builder,
    @Value("\${feedrelay.google-tasks-base-url:https://tasks.googleapis.com/tasks/v1}") baseUrl: String,
) : DestinationAdapter {

    private val restClient = restClientBuilder.baseUrl(baseUrl).build()

    override val type = DestinationType.GOOGLE_TASKS

    override fun snapshot(token: BearerToken, target: TargetRef): DestinationSnapshot {
        val byRef = mutableMapOf<ExternalRef, DestinationItemState>()
        var pageToken: String? = null
        do {
            val page = restClient.get()
                .uri { builder ->
                    builder.path("/lists/{tasklist}/tasks")
                        .queryParam("showCompleted", "true")
                        .queryParam("showHidden", "true")
                        .queryParam("maxResults", "100")
                        .apply { pageToken?.let { queryParam("pageToken", it) } }
                        .build(target.value)
                }
                .headers { it.setBearerAuth(token.value) }
                .retrieve()
                .body(TaskListResponse::class.java)
            page?.items.orEmpty().forEach { task ->
                val state = if (task.status == "completed") DestinationItemState.COMPLETED else DestinationItemState.OPEN
                byRef[externalRef(target, task.id)] = state
            }
            pageToken = page?.nextPageToken
        } while (pageToken != null)
        return DestinationSnapshot(byRef)
    }

    override fun create(token: BearerToken, target: TargetRef, item: OutboundItem, zone: ZoneId): ExternalRef {
        val created = restClient.post()
            .uri("/lists/{tasklist}/tasks", target.value)
            .headers { it.setBearerAuth(token.value) }
            .body(taskBody(item, zone))
            .retrieve()
            .body(TaskResponse::class.java)
        return externalRef(target, checkNotNull(created?.id) { "생성 응답에 id 없음" })
    }

    override fun update(token: BearerToken, ref: ExternalRef, item: OutboundItem, zone: ZoneId) {
        val (tasklistId, taskId) = parse(ref)
        restClient.patch()
            .uri("/lists/{tasklist}/tasks/{task}", tasklistId, taskId)
            .headers { it.setBearerAuth(token.value) }
            .body(taskBody(item, zone))
            .retrieve()
            .toBodilessEntity()
    }

    override fun listTargets(token: BearerToken): List<TargetInfo> {
        val response = restClient.get()
            .uri("/users/@me/lists")
            .headers { it.setBearerAuth(token.value) }
            .retrieve()
            .body(TasklistsResponse::class.java)
        return response?.items.orEmpty().map { TargetInfo(ref = TargetRef(it.id), name = it.title) }
    }

    override fun createTarget(token: BearerToken, name: String): TargetRef {
        val created = restClient.post()
            .uri("/users/@me/lists")
            .headers { it.setBearerAuth(token.value) }
            .body(mapOf("title" to name))
            .retrieve()
            .body(TasklistResponse::class.java)
        return TargetRef(checkNotNull(created?.id) { "리스트 생성 응답에 id 없음" })
    }

    private fun taskBody(item: OutboundItem, zone: ZoneId): Map<String, String> {
        val body = mutableMapOf("title" to item.title, "notes" to managedNotes(item, zone))
        item.dueAt?.let {
            // Tasks due는 날짜만 유효 — 사용자 타임존 기준 날짜의 UTC 자정으로 표기
            body["due"] = "${it.atZone(zone).toLocalDate()}T00:00:00.000Z"
        }
        return body
    }

    /** notes는 관리 필드 (§1) — 마감 시각·원본 링크·본문을 우리가 소유하고 갱신 시 덮어쓴다 */
    private fun managedNotes(item: OutboundItem, zone: ZoneId): String =
        listOfNotNull(
            item.dueAt?.let { "마감: ${DUE_FORMAT.format(it.atZone(zone))}" },
            item.url?.let { "원본: $it" },
            item.notes?.takeIf { it.isNotBlank() },
        ).joinToString("\n")

    private fun externalRef(target: TargetRef, taskId: String) = ExternalRef("${target.value}/$taskId")

    private fun parse(ref: ExternalRef): Pair<String, String> {
        val separator = ref.value.lastIndexOf('/')
        require(separator > 0) { "잘못된 ExternalRef: ${ref.value}" }
        return ref.value.substring(0, separator) to ref.value.substring(separator + 1)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TaskListResponse(val items: List<TaskResponse>?, val nextPageToken: String?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TaskResponse(val id: String, val status: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TasklistsResponse(val items: List<TasklistResponse>?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TasklistResponse(val id: String, val title: String = "")

    companion object {
        private val DUE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm zzz")
    }
}
