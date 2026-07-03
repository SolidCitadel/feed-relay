package com.feedrelay.identity.internal.application.port.`in`

/** 가입/재로그인 — google_sub 기준 upsert, 프로필은 매 로그인 동기화 (§6.2). 반환: userId */
interface UpsertUserUseCase {
    fun upsert(command: UpsertUserCommand): Long
}

data class UpsertUserCommand(
    val googleSub: String,
    val email: String,
    val displayName: String?,
)
