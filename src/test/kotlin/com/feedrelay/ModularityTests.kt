package com.feedrelay

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/**
 * 모듈 경계 검증 — docs/01-design.md 1절의 의존 규칙을 기계적으로 강제한다.
 * Spring 컨텍스트·DB 불필요 (Docker 없이 실행됨).
 */
class ModularityTests {

	@Test
	fun verifyModuleBoundaries() {
		ApplicationModules.of(FeedrelayApplication::class.java).verify()
	}
}
