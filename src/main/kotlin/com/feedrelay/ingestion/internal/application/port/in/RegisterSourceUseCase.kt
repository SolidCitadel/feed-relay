package com.feedrelay.ingestion.internal.application.port.`in`

import com.feedrelay.ingestion.api.ItemKind
import java.time.Instant

/** 소스 등록 — Feed를 즉시 수집해 검증하고 항목 미리보기를 돌려준다 (§6.2) */
interface RegisterSourceUseCase {
    fun register(command: RegisterSourceCommand): RegisteredSource
}

data class RegisterSourceCommand(
    val userId: Long,
    val name: String,
    val url: String,
)

data class RegisteredSource(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val preview: List<PreviewItem>,
)

data class PreviewItem(
    val title: String,
    val kind: ItemKind,
    val startAt: Instant?,
)
