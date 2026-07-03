package com.feedrelay.identity.api

/**
 * 로그인 시 identity가 세션 주체(principal)에 심는 내부 식별자.
 * 다른 모듈의 웹 어댑터는 identity를 호출하지 않고 principal 캐스팅으로 userId를 얻는다.
 */
interface AuthenticatedUser {
    val userId: Long
}
