# 4. 해법 전략

| 전략 | 근거 |
|---|---|
| 모듈러 모놀리스 (Spring Modulith) + Docker Compose. MQ·Redis·k3s는 트리거 정의 후 보류 | [ADR-0001](../adr/0001-modular-monolith-and-compose.md) |
| 단일 바운더리 컨텍스트 + 서브도메인 정렬 모듈 7개 | [ADR-0004](../adr/0004-single-bounded-context.md) |
| 계약 타입은 생산자 소유(shared 없음), 사실(SourceItem)/의도(OutboundItem) 타입 분리 | [ADR-0005](../adr/0005-producer-owned-contracts.md) |
| 통신 2평면: 데이터=오케스트레이션 직접 호출, 통지=애플리케이션 이벤트 | [ADR-0006](../adr/0006-two-plane-communication.md) |
| 모듈 내부는 전 모듈 동일한 정식형 헥사고날, CQRS는 인바운드 포트 명명 | [ADR-0007](../adr/0007-canonical-hexagonal-internals.md) |
| React SPA를 Spring이 동일 오리진에서 정적 서빙 (세션 쿠키 유지) | [ADR-0008](../adr/0008-react-same-origin.md) |
| 잡큐는 Postgres `FOR UPDATE SKIP LOCKED`, 스케줄링은 @Scheduled+ShedLock | ADR-0001 |
| 스택: Kotlin · Spring Boot 4 · PostgreSQL+Flyway · React(Vite) · JDK 21 | — |
