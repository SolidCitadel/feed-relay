# 5. 빌딩 블록 뷰

## Level 1 — 모듈 (단일 컨텍스트 안의 서브도메인 정렬 패키지)

```
com.feedrelay
├── identity      # [generic]    사용자 계정·세션 (Google 로그인)
├── connections   # [generic]    위임 그랜트: 토큰 암호화 저장·갱신
├── ingestion     # [supporting] 소스 등록·수집·정규화 — 산출 계약 SourceItem
├── rules         # [core]       규칙·템플릿 정의와 평가 — 산출 계약 OutboundItem
├── delivery      # [supporting] 대상 앱 반영 (무상태 어댑터)
├── sync          # [core]       Subscription 계약·SyncMapping 이행·실행 조립
├── notifications # [generic]    통지 이벤트 구독 → Discord webhook (api 없음)
└── (root)        # Boot 메인 + 횡단 설정 — Modulith 관례상 open
```

| 모듈 | 서브도메인 | 책임 (요약) | 소유 애그리거트 / 테이블 | 산출 계약 |
|---|---|---|---|---|
| `identity` | generic | 사용자 계정·세션 (인증: 누구인가) | User / users | — |
| `connections` | generic | 위임 그랜트·토큰 수명 (위임: 무엇을 대신하나) | Connection / connections | — |
| `ingestion` | supporting | Source 등록, Feed 수집·정규화 | Source / sources | **SourceItem** |
| `rules` | **core** | 규칙·템플릿의 정의와 평가 | RuleSet / rule_sets (+Template: 리소스 파일) | **OutboundItem** |
| `delivery` | supporting | 대상 앱 반영 (무상태) | 없음 | — |
| `sync` | **core** | Subscription 계약·SyncMapping 이행·실행 조립 | Subscription, SyncMapping, RunLog | — |
| `notifications` | generic | 통지 이벤트 구독 → 운영자 알림 | 없음 | — |

- 모듈 ≠ 애그리거트(모듈당 0..N개). SlotMapping은 Subscription 내부 VO, SyncMapping은 독립 애그리거트(항목 단위 갱신 — 묶으면 통째-로드 규칙 자멸), RunLog는 append-only 독립. 애그리거트 간 참조는 id로만.
- core/supporting/generic = 설계·테스트 투자 우선순위. 차별점은 rules·sync.

## 의존 규칙 (Modulith 테스트로 강제 — `ModularityTests`)

```
런타임 호출   : sync → identity, ingestion, rules, delivery, connections  (조립은 sync만 — identity는 사용자 설정(타임존) 조회)
계약 타입 의존: rules →(SourceItem)→ ingestion / delivery →(OutboundItem)→ rules
               파이프라인 방향으로만. 역방향 금지. shared 패키지 없음 — 계약은 생산자 소유.
통지(이벤트)  : sync ⇢ RunCompleted·RunFailed (→ notifications)
               connections ⇢ ConnectionRevoked (→ sync)
               발행자는 수신자를 모른다. 이벤트 타입은 발행자 api 소속.
delivery ─X─ connections : 토큰은 sync가 값으로 전달 (갱신 책임은 connections)
인증 주체     : identity가 로그인 시 principal에 userId를 강화(api의 AuthenticatedUser) —
               각 모듈 웹 어댑터는 캐스팅으로 참조, identity 런타임 호출 불필요
```

## Level 2 — 모듈 내부 (전 모듈 동일, 정식형 헥사고날)

```
<module>/
├── api/                 # @NamedInterface — 계약 타입·파사드·발행 이벤트
└── internal/
    ├── domain/          # 애그리거트·VO·도메인 로직
    ├── application/
    │   ├── port/in/     # XxxUseCase(쓰기) / XxxQuery(읽기) — C/Q는 명명으로 구분
    │   ├── port/out/    # LoadSubscriptionPort, RecordRunPort …
    │   └── service/     # 포트 구현 — 트랜잭션 경계
    └── adapter/
        ├── in/…         # 구동자별: web, scheduler, event
        └── out/…        # 기술별: persistence, googletasks, googleauth, discord
```

- 구동자(사용자/시계/이벤트)는 인바운드 어댑터의 속성 — 코어는 무관심.
- 폴더는 첫 클래스와 함께 생성(빈 폴더 금지). **조립용 전역 web 모듈 없음** — 온보딩 각 단계가 정확히 한 모듈의 오퍼레이션에 대응, 플로우 조립은 React.
- 모듈별 어댑터 예: delivery=adapter/out/googletasks(domain 없음), connections=adapter/in/web(OAuth 콜백)+adapter/out/googleauth, notifications=adapter/in/event+adapter/out/discord.
