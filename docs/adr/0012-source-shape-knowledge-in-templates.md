# ADR-0012: 소스 형태 지식은 템플릿이 단일 소재로 소유

- 날짜: 2026-07-03 / 상태: Accepted

## 맥락

Canvas는 과제도 VEVENT로 내보내므로(§8.7) 과제/일정 판별에는 Canvas 고유 지식(UID 패턴)이 필요하다. 기존 서술은 SourceItem.kind를 "의미 분류"로 정의하면서 판별의 소재(모듈)는 미규정이었다. SUMMARY 포맷 지식은 이미 템플릿(rules 소유)에 있으므로, 판별을 ingestion에 두면 한 소스의 형태 지식이 두 모듈로 분산된다. M1 구현이 이 갈림길의 결정을 강제했다.

## 검토한 대안

1. **ingestion이 판별**: ical 어댑터(또는 소스별 정규화기)가 UID 패턴으로 kind=TASK/EVENT를 산출. 새 LMS 지원 시 템플릿 추가 + ingestion 코드 수정이 모두 필요 — "템플릿 수정·추가만으로 소스 차이를 흡수한다"는 제품 목적과 충돌. 어댑터가 프로토콜(ical)뿐 아니라 벤더(Canvas)까지 알게 된다.
2. **템플릿이 판별** (채택): ical 어댑터는 순수 RFC 5545 파싱 — kind는 컴포넌트 유래 사실(VEVENT→EVENT, VTODO→TASK). 벤더 지식(UID 패턴·SUMMARY 포맷)은 전부 템플릿 정규식. 분류는 core 모듈 rules의 본연 책임이고, 템플릿은 어차피 소스 형태별(canvas-v1)로 존재하므로 추가 비용이 없다.

## 결정

- SourceItem.kind의 의미를 "의미 분류" → **컴포넌트 유래 사실**로 재정의 (§12에 ItemKind 등재).
- 어댑터는 프로토콜 단위(ical, 추후 webhook/scraper)로만 존재하고 벤더를 모른다. 벤더 지식의 단일 소재는 템플릿.
- canvas-v1: `uid` 정규식 `^event-calendar-event-` 매치 → exclude 규칙을 최상단 배치(first-match-wins — 일정도 `[course_code]` SUMMARY를 갖기 때문).

## 결과

- 새 LMS/벤더 지원 = 템플릿 추가로 완결. 어댑터 증가는 프로토콜 추가 시에만.
- 트레이드오프: 판별력이 정규식 표현력에 갇힌다. 재검토 트리거 — 정규식으로 판별 불가능한 소스(구조적 필드 조합 등)가 등장하면 템플릿 정의에 비정규식 매처 추가를 우선 검토.
