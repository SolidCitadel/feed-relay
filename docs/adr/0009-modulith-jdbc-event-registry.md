# ADR-0009: Modulith 이벤트 발행 레지스트리는 JDBC 방식

- 날짜: 2026-07-03 / 상태: Accepted

## 맥락

M0 빌드에서 Initializr 기본값(spring-modulith-starter-jpa)이 이벤트 발행 레지스트리를 JPA 엔티티로 관리 → `event_publication` 테이블을 요구해 `ddl-auto=validate`와 충돌(Flyway 마이그레이션 부재). 통지 평면(ADR-0006)의 at-least-once 보장에 레지스트리는 필요.

## 검토한 대안

1. JPA 레지스트리 유지 + Flyway로 테이블 DDL 수작성: Modulith 내부 엔티티 스키마를 손으로 추적 — 버전업 취약
2. **JDBC 레지스트리**(spring-modulith-starter-jdbc, 채택): 자체 스키마 초기화(`spring.modulith.events.jdbc.schema-initialization.enabled=true`) 제공, JPA 엔티티 없음

## 결정

starter-jdbc + 스키마 자동 초기화. 도메인 스키마는 Flyway가, 프레임워크 부속 테이블은 프레임워크가 소유.

## 결과

- ddl-auto=validate는 도메인 엔티티만 검증 — 경계 명확.
- 재검토 트리거: 이벤트 externalization(브로커 승격) 도입 시 레지스트리 구성 재점검.
