# 8. 횡단 개념

## 8.1 도메인 계약 (생산자 소유 — ADR-0005)

```kotlin
// ingestion.api — 소스가 말한 "사실"
data class SourceItem(
    val sourceUid: String,          // ical UID — 멱등성의 기준 키
    val kind: ItemKind,             // EVENT, TASK — 의미 분류 (컴포넌트 타입 아님, 8.7 참조)
    val title: String,
    val description: String?,
    val url: String?,
    val dueAt: Instant?,
    val startAt: Instant?, val endAt: Instant?,
    val raw: Map<String, String>,   // 정규식 매칭 대상 원본 필드
)

// ingestion.api
interface SourceAdapter {
    val type: SourceType                                  // ICAL (추후 WEBHOOK, SCRAPER)
    fun fetch(config: SourceConfig): List<SourceItem>
}

// rules.api — 대상에 쓰려는 "의도" (사실과 타입으로 구분 — 변환 전 해싱·배달을 타입이 차단)
data class OutboundItem(val title: String, val notes: String?, val dueAt: Instant?, val url: String?)

interface RuleEngine {
    fun evaluate(item: SourceItem, ruleSet: RuleSet): RuleOutcome
}
sealed interface RuleOutcome {
    data class Route(val slot: String, val outbound: OutboundItem) : RuleOutcome
    object Exclude : RuleOutcome
}

// delivery.api
interface DestinationAdapter {
    val type: DestinationType                             // GOOGLE_TASKS (추후 TODOIST, GOOGLE_CALENDAR)
    fun snapshot(token: BearerToken, target: TargetRef): DestinationSnapshot   // 리스트당 1콜
    fun create(token: BearerToken, target: TargetRef, item: OutboundItem): ExternalRef
    fun update(token: BearerToken, ref: ExternalRef, item: OutboundItem)
}
```

## 8.2 규칙 정의 (rule_sets.definition_json)

```json
{
  "version": 1,
  "rules": [
    { "id": "extract-course",
      "match": { "field": "summary", "regex": "^(?<title>.+?)\\s*\\[(?<course>.+)\\]$" },
      "action": { "type": "route", "slot": "${course}", "transform": { "title": "${title}" } } },
    { "id": "drop-events",
      "match": { "field": "kind", "regex": "^EVENT$" }, "action": { "type": "exclude" } }
  ],
  "fallback": { "type": "route", "slot": "_inbox" }
}
```

- first-match-wins. slot은 캡처 치환 결과, 구독의 `slot_mapping_json`이 slot→TargetRef 연결.
- **ReDoS 가드**: 매칭 타임아웃(100ms)·입력 길이 제한을 엔진 차원에서 (v2 사용자 정규식 대비).

## 8.3 데이터 모델 (ERD)

```
users          (id PK, google_sub UNIQUE, email, display_name, created_at)
connections    (id PK, user_id FK, provider, scopes, access_token_enc, refresh_token_enc, expires_at, status)
sources        (id PK, user_id FK, type, name, config_json, status)
rule_sets      (id PK, user_id FK, origin_template_key, origin_template_version, definition_json, …)
subscriptions  (id PK, user_id FK, source_id, rule_set_id, connection_id, destination_type,
                slot_mapping_json, next_run_at, status [ACTIVE|PAUSED|ERROR], last_run_at)
sync_mappings  (id PK, subscription_id FK, source_uid, external_ref, content_hash,
                status [ACTIVE|FROZEN], frozen_reason [COMPLETED|DELETED]?, …
                UNIQUE (subscription_id, source_uid))
run_logs       (id PK, subscription_id FK, started_at, finished_at, result [SUCCESS|PARTIAL|FAILED], stats_json, error_summary)
```

- 테이블 소유권: §5 표. 다른 모듈 테이블은 FK id 참조만 — 직접 조인·쓰기 금지.
- **content_hash = SHA-256(OutboundItem + TargetRef)** — 변환 후 기준. transform 변경은 내용만 갱신, 위치는 external_ref가 고정 (ADR-0003).
- 템플릿은 DB가 아니라 리소스 파일(JSON) — 선택 시 rule_sets로 복제.

## 8.4 영속성

- 도메인 모델·영속 모델 겸용(domain에 JPA 허용 — no-mapping 전략, ADR-0007). 순수 로직(RuleEngine)은 JPA 비의존.
- 도메인 스키마는 Flyway 소유, 프레임워크 부속 테이블(이벤트 레지스트리)은 프레임워크 소유 (ADR-0009).
- `ddl-auto=validate`, `open-in-view=false`.

## 8.5 보안

- 인증: Google OAuth — identity(로그인: openid/email/profile)와 connections(위임: tasks, incremental+offline)는 다른 관심사 ([§12 인증 계열](12-glossary.md)).
- 세션: 쿠키 host-only + Secure + SameSite=Lax (PSL 미등재 무료 도메인 대비 상위 도메인 쿠키 금지). Spring Session JDBC(M2) — 재배포에도 유지.
- CSRF: SPA + 세션 쿠키 → XSRF-TOKEN 쿠키 방식.
- 토큰 보관: refresh_token AES-GCM 암호화, 키는 환경변수(.env — 저장소 밖).

## 8.6 스케줄링·이벤트

- 잡큐: Postgres `FOR UPDATE SKIP LOCKED` + `@Scheduled`(ShedLock). MQ 도입 트리거 = 웹훅 소스/멀티 워커.
- 통지 이벤트: `@ApplicationModuleListener`(커밋 후 비동기) + JDBC 발행 레지스트리(at-least-once) — 리스너는 멱등이어야 함.
- **명령/사실 판별 테스트** (새 통신 추가 시): ① 제2의 무관한 리스너가 상상되는가 — 아니오면 명령(직접 호출) ② 다음 분기가 결과에 의존하는가 — 예면 명령 ③ 흐름 전체 불변식과 소유자가 있는가 — 예면 오케스트레이션. 특정 반응을 기대하는 이벤트("이벤트 옷을 입은 명령") 금지 ([ADR-0006](../adr/0006-two-plane-communication.md)).

## 8.7 Canvas 템플릿 (canvas-v1)

- 입력: Canvas 캘린더 피드(.ics). 공식 문서에 피드 스키마 서술은 없음 — **스키마 원천은 canvas-lms 소스** ([`app/models/calendar_event.rb`의 `IcalEvent`](https://github.com/instructure/canvas-lms/blob/master/app/models/calendar_event.rb), 2026-07 확인):
  - 전부 VEVENT (VTODO 없음). UID = `event-{모델명 케밥케이스}-{id}` — `event-assignment-…`(과제) / `event-sub-assignment-…`(체크포인트형 과제) / `event-calendar-event-…`(일정)
  - `SUMMARY = 제목 [course_code]` — 붙는 것은 과목 **코드**(코스의 course_code 필드). 인스턴스가 코드에 뭘 넣는지(한글 과목명 vs 학수번호)는 관측으로 확정 — 템플릿 정규식 조정으로 흡수
  - 과제: DTSTART=DTEND=due_at(DateTime, UTC). 종일 이벤트: DATE 타입 + DTEND 생략(RFC 5545) — 파서는 두 형태 모두 수용
  - DESCRIPTION은 HTML→텍스트 변환본, HTML 원본은 `X-ALT-DESC`에 보존
- **kind는 의미 분류**: LMS는 과제도 VEVENT로 내보냄(주요 캘린더 앱의 VTODO 미지원 탓) → 과제/일정 판별은 UID 패턴(`assignment` 계열 → TASK). EVENT 제외해도 과제 누락 없음.
- 검증 경로: 구현·테스트는 소스 유래 **fixture .ics**로, E2E는 Free-for-Teacher 계정(canvas.instructure.com) 실피드로 상시 가능. 학교 실피드(Canvas 확인됨) 차이는 학기 재개 시 템플릿 수정으로 흡수.
- 능력 격차 흡수: Google Tasks due는 날짜만 → 시각·원본 링크는 notes에 보존.

## 8.8 테스트 전략

- **멱등성 시나리오가 1급 시민**: §10의 시나리오를 가짜 ical 서버(MockWebServer)+가짜 Tasks API 통합 테스트로 고정.
- Modulith 경계 검증(`ModularityTests` — Docker 불요), Testcontainers(PG — Docker 없으면 자동 스킵).
