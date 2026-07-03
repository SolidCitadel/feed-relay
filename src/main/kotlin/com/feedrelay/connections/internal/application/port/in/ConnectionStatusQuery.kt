package com.feedrelay.connections.internal.application.port.`in`

/** 대시보드·마법사용 위임 상태 조회 */
interface ConnectionStatusQuery {
    fun googleStatusFor(userId: Long): ConnectionStatusView?
}

data class ConnectionStatusView(
    val status: String,
    val scopes: String,
)
