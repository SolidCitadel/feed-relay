package com.feedrelay.identity.internal.application.service

import com.feedrelay.identity.internal.application.port.`in`.UpsertUserCommand
import com.feedrelay.identity.internal.application.port.out.LoadUserPort
import com.feedrelay.identity.internal.application.port.out.SaveUserPort
import com.feedrelay.identity.internal.domain.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserServiceTests {

    private class InMemoryUserStore : LoadUserPort, SaveUserPort {
        val byGoogleSub = mutableMapOf<String, User>()
        private var nextId = 1L
        override fun findByGoogleSub(googleSub: String): User? = byGoogleSub[googleSub]
        override fun findById(id: Long): User? = byGoogleSub.values.firstOrNull { it.id == id }
        override fun save(user: User): User {
            if (user.id == null) {
                // JPA identity 생성 흉내 — id는 val이라 리플렉션으로 부여
                User::class.java.getDeclaredField("id").apply { isAccessible = true }.set(user, nextId++)
            }
            byGoogleSub[user.googleSub] = user
            return user
        }
    }

    private val store = InMemoryUserStore()
    private val service = UserService(store, store)

    @Test
    fun `첫 로그인이면 사용자를 생성한다`() {
        service.upsert(UpsertUserCommand("sub-1", "a@example.com", "홍길동"))

        val user = store.byGoogleSub.getValue("sub-1")
        assertEquals("a@example.com", user.email)
        assertEquals("홍길동", user.displayName)
    }

    @Test
    fun `재로그인이면 프로필을 클레임으로 동기화한다 - 구글이 진실 원천`() {
        service.upsert(UpsertUserCommand("sub-1", "a@example.com", "홍길동"))
        service.upsert(UpsertUserCommand("sub-1", "b@example.com", "홍길동2"))

        assertEquals(1, store.byGoogleSub.size)
        val user = store.byGoogleSub.getValue("sub-1")
        assertEquals("b@example.com", user.email)
        assertEquals("홍길동2", user.displayName)
    }

    @Test
    fun `없는 sub 조회는 null`() {
        assertNull(service.findByGoogleSub("ghost"))
    }

    @Test
    fun `조회는 UserView로 프로필만 노출한다`() {
        service.upsert(UpsertUserCommand("sub-1", "a@example.com", null))

        val view = service.findByGoogleSub("sub-1")
        assertEquals("a@example.com", view?.email)
        assertNull(view?.displayName)
    }

    @Test
    fun `타임존 기본값은 KST이고 변경 가능하다 - 재로그인에도 유지`() {
        service.upsert(UpsertUserCommand("sub-1", "a@example.com", "홍길동"))
        assertEquals("Asia/Seoul", service.findByGoogleSub("sub-1")?.timezone)

        service.update("sub-1", java.time.ZoneId.of("America/New_York"))
        service.upsert(UpsertUserCommand("sub-1", "a@example.com", "홍길동")) // 재로그인 동기화

        assertEquals("America/New_York", service.findByGoogleSub("sub-1")?.timezone)
    }
}
