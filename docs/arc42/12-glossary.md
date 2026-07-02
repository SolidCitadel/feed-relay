# 12. 용어집 (유비쿼터스 언어)

> 용어와 언어 경계 판정 기준의 **단일 원천**. arc42 문서·코드 식별자·ADR 서술·대화가 이 정의를 따른다. 동일 개념에 다른 표현을 쓰지 않고, 새 개념은 여기에 정의를 추가한 뒤 사용한다.

## 판정 기준

**용어 = 이 시스템의 언어 안에서 단 하나의 정의와 불변식을 갖도록 구속한 말.** 판정 세 가지: ① 한 문장의 정의를 쓸 수 있다 ② 거시 동작([§1](01-introduction-and-goals.md))에 등장하거나 그것을 정밀화한다 ③ 코드·문서·대화에서 그 정의로만 쓰인다.

- 일반명사("항목", "계정", "목적지")는 용어가 아니다. 정밀함이 필요해지는 지점에서 용어를 신조해 대체한다.
- 일반명사의 다의성은 경계의 증거가 아니라 **용어 신조의 신호**다. 신조로 해소되면 언어는 하나로 유지된다.
- **언어(바운더리 컨텍스트)가 갈라지는 조건**: 같은 용어가 양립 불가능한 두 정의를 요구하고 개명으로 해소되지 않을 때(별도 팀, 인수 레거시 등). 모듈이 커진다는 이유로는 갈라지지 않는다. 현재 판정 결과(단일 컨텍스트·외부 언어 경계 2곳)는 [§3](03-context-and-scope.md)·[§5](05-building-blocks.md), 경위는 [ADR-0004](../adr/0004-single-bounded-context.md).

## 용어 사전

### 수집 계열
- `Source`: 사용자가 등록한 수집처 엔티티 (예: "내 Canvas 캘린더" — config에 ical URL).
- `Feed`: Source가 가리키는 외부 세계의 원본 문서 (ics 파일 그 자체).
- `SourceItem`: Feed를 정규화한 개별 값 객체 — **소스가 말한 사실**의 진술.
- `ItemKind`: SourceItem의 컴포넌트 유래 종별 사실 (VEVENT→EVENT, VTODO→TASK). 과제/일정의 의미 판별이 아니다 — 그것은 템플릿 규칙의 산출이다 ([ADR-0012](../adr/0012-source-shape-knowledge-in-templates.md)).
- 차이점: Source는 영속 엔티티, Feed는 외부 문서, SourceItem은 실행 중에만 흐르는 값(영속되지 않음).

### 사실과 의도
- `SourceItem`: 소스가 말한 **사실** (변환 전 — title은 원문 그대로).
- `OutboundItem`: rules가 산출한, 대상에 쓰려는 **의도** (변환 후 — title은 우리의 표현).
- 차이점: 필드 모양이 비슷해도 진술의 성격이 다르다. content_hash와 delivery의 대상은 항상 OutboundItem이다. 변환 전 값을 해싱·배달하는 버그를 타입으로 차단한다.

### 재료와 계약
- `Connection`: 무엇을 대신할 수 있는가 (위임 권한).
- `Source`: 어디서 오는가 (입력).
- `Subscription`: 무엇을 어디로 — Source × RuleSet × Connection × 대상의 조합 계약. 애그리거트 루트.
- 차이점: Connection과 Source는 재료일 뿐 흐름을 만들지 않는다. 주기 실행이라는 흐름은 Subscription만이 만든다.

### 라우팅 계열
- `Slot`: 규칙이 산출하는 추상 분배 이름 (예: 과목명 "자료구조", 폴백 `_inbox`).
- `SlotMapping`: Subscription이 보유한 Slot → TargetRef 대응표.
- `TargetRef`: 대상 앱 안의 구체 위치 (예: Google Tasks의 tasklistId).
- `ExternalRef`: 대상 앱에 생성된 개별 항목의 id.
- 차이점: rules는 Slot까지만 안다(추상 유지). 구체화는 sync의 SlotMapping이, 개별 추적은 SyncMapping의 ExternalRef가 담당한다. '목적지'라는 일반명사는 모델 어휘가 아니다 — 항상 셋 중 무엇인지 특정해서 말한다.

### 규칙 계열
- `Template`: 배포되는 읽기 전용 규칙 원본 (리소스 파일, 예: canvas-v1). 코드의 일부.
- `RuleSet`: Template을 복제한 사용자 소유 규칙 묶음 (편집 가능, origin key/version 기록).
- `Rule`: RuleSet 안의 단일 매치-액션 단위.
- `Match`: 필드 + 정규식 매칭 조건 (캡처 그룹 포함).
- `Action`: 매치 시 행위 — `route`(Slot 지정 + Transform) 또는 `exclude`.
- `Transform`: 캡처 치환으로 필드를 재작성하는 정의.
- `RuleSetDefinition`: Template과 RuleSet이 공용하는 규칙 정의 값 객체 (version·rules·fallback — JSON 직렬화 형태). RuleEngine의 입력.
- 차이점: Template은 배포 단위(코드), RuleSet은 사용자 데이터 — 둘 다 내용물은 RuleSetDefinition 하나로 표현된다. 평가는 first-match-wins로 Rule 순서에 의존한다.

### 인증 계열
- `identity의 OAuth`: **인증** — 사용자가 누구인지 확인 (openid/email/profile, 가입 시).
- `connections의 OAuth`: **위임** — 사용자를 대신할 권한 획득 (tasks 등, 연동 시 incremental).
- 차이점: 같은 Google OAuth 프로토콜이지만 다른 개념이다. 로그인했다고 위임이 생기지 않으며, 요청 시점도 분리된다.

### 동기화 상태 계열
- `SyncMapping`: (Subscription, sourceUid) → ExternalRef + content_hash + 상태. 항목 단위 영속 상태 — 멱등성의 단위.
- `content_hash`: OutboundItem + TargetRef의 해시. 변경 감지 기준.
- `FROZEN`: 이후 불개입 상태 (사유 COMPLETED=대상에서 완료됨 / DELETED=대상에서 삭제됨).
- `Run`: Subscription 1회 실행. RunLog로 이력화.
- 차이점: SyncMapping은 항목의 현재 상태(계속 갱신), RunLog는 실행의 과거 이력(불변 append). 단위도 다르다 — 항목 vs 실행.

### 통지 계열
- `RunCompleted` / `RunFailed`: sync가 Run 종료 시 발행하는 사실 통지 이벤트. 발행 후 발행자의 분기는 변하지 않는다.
- `ConnectionRevoked`: connections가 위임 철회를 감지하면 발행 — sync가 구독해 해당 Subscription을 ERROR 처리한다.
- 차이점: 통지 이벤트는 데이터 평면(직접 호출·동기 응답)과 달리 응답이 없으며, 커밋 후 비동기 at-least-once로 전달된다. 명령/사실 판별 테스트는 [§8](08-crosscutting-concepts.md).

## 폐기 용어

- `syncstate`, `pipeline`(모듈명), `web`(모듈명): sync 통합·해체로 소멸. "파이프라인"은 실행 흐름을 가리키는 일반명사로만 쓴다.
- `Ingestion/Classification/Export`(구상 초안 명칭) → ingestion / rules / delivery.
- **"바운더리 컨텍스트"(내부 분할 지칭으로서)**: 내부 분할은 컨텍스트가 아니라 **모듈**이다. 컨텍스트는 시스템 전체(단일)와 외부 세계(ical, Google Tasks)에만 쓴다.
- `shared`(패키지), "공유 커널", "공유 데이터 계약": 폐기 — 계약 타입은 생산자 모듈이 소유한다.
- `엔진`: 폐기 — 구동자(사용자/시계/이벤트)는 인바운드 어댑터의 속성일 뿐, 별도 범주가 아니다.
