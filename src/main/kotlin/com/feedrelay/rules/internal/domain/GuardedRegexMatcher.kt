package com.feedrelay.rules.internal.domain

import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * ReDoS 가드가 걸린 정규식 매칭 (§8.2, Q10) — 어떤 실패도 "매치 안 됨"으로 수렴시켜 실행을 계속한다.
 *
 * - 매치당 100ms 타임아웃: 백트래킹 폭발을 입력 문자 접근 시점의 데드라인 검사로 중단.
 * - 입력 길이 상한: 자르면 앵커(^$) 의미가 왜곡되므로 자르지 않고 매치 불가로 처리.
 * - 정규식 문법 오류(v2 사용자 정규식 대비): 매치 불가 + 경고.
 */
object GuardedRegexMatcher {

    private const val TIMEOUT_MILLIS = 100L
    private const val MAX_INPUT_LENGTH = 10_000

    private val NAMED_GROUP = Regex("""\(\?<([a-zA-Z][a-zA-Z0-9]*)>""")
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 부분 매치(find) 시 이름 있는 캡처 그룹의 값 맵을, 미매치·가드 발동 시 null을 반환한다.
     * 전체 일치가 필요한 규칙은 정규식에 앵커(^$)를 명시한다.
     */
    fun match(regex: String, input: String): Map<String, String>? {
        if (input.length > MAX_INPUT_LENGTH) {
            log.warn("입력 길이 {} > 상한 {} — 매치 불가 처리", input.length, MAX_INPUT_LENGTH)
            return null
        }
        val pattern = try {
            Pattern.compile(regex)
        } catch (e: PatternSyntaxException) {
            log.warn("정규식 문법 오류 — 매치 불가 처리: {}", e.message)
            return null
        }
        val deadline = System.nanoTime() + TIMEOUT_MILLIS * 1_000_000
        val matcher = pattern.matcher(DeadlineCharSequence(input, deadline))
        return try {
            if (!matcher.find()) return null
            NAMED_GROUP.findAll(regex)
                .map { it.groupValues[1] }
                .distinct()
                .mapNotNull { name -> matcher.group(name)?.let { name to it } }
                .toMap()
        } catch (e: MatchTimeoutException) {
            log.warn("정규식 매치 {}ms 타임아웃 — 매치 불가 처리: {}", TIMEOUT_MILLIS, regex)
            null
        }
    }

    private class MatchTimeoutException : RuntimeException(null, null, false, false)

    private class DeadlineCharSequence(
        private val delegate: CharSequence,
        private val deadlineNanos: Long,
    ) : CharSequence {
        override val length get() = delegate.length

        override fun get(index: Int): Char {
            if (System.nanoTime() > deadlineNanos) throw MatchTimeoutException()
            return delegate[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            DeadlineCharSequence(delegate.subSequence(startIndex, endIndex), deadlineNanos)

        // Matcher.group()이 subSequence().toString()으로 캡처 값을 만들므로 필수
        override fun toString(): String = delegate.toString()
    }
}
