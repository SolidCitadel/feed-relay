package com.feedrelay.connections.internal.application.port.`in`

import java.time.Instant

/** 위임 완료(콜백) 시 그랜트 저장 — (userId, provider) 기준 upsert */
interface RegisterConnectionUseCase {
    fun register(command: RegisterConnectionCommand)
}

data class RegisterConnectionCommand(
    val userId: Long,
    val scopes: Set<String>,
    val accessToken: String,
    val accessTokenExpiresAt: Instant?,
    val refreshToken: String,
)
