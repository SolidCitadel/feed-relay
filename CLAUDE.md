# FeedRelay

## 문서 규칙
- 설계·현황에 닿는 답변·작업은 추론하지 말고 `docs/arc42/`(01 목표 ~ 12 용어)를 먼저 읽는다. 결정의 맥락·대안·근거는 `docs/adr/`에 있다.
- **아키텍처 결정**이 새로 생기거나 바뀌면 ADR을 먼저 쓴다 (생성 기준·수명주기는 `docs/adr/README.md`). 결정을 동반하지 않는 명료화·세부 보강은 arc42 직접 수정.
- 용어·언어 경계 판정 기준은 `docs/arc42/12-glossary.md`가 단일 원천이다. 코드·문서·대화에서 동일 개념에 다른 표현을 쓰지 않고, 새 개념은 거기 정의를 추가한 뒤 사용한다.

## 작업 규칙
- 커밋: Conventional Commits — 타입·스코프는 영어(`feat(sync): …`), 설명·본문은 한국어.
- 코드: 식별자는 용어 사전의 영어 용어, 주석·KDoc은 한국어.
- 시크릿은 환경변수(`.env` — gitignore됨)로만 다룬다 — OAuth client secret·토큰 암호화 키 등을 설정 파일·코드에 하드코딩하지 않는다.

## 빌드·검증
- `./gradlew build` — frontend(Vite) 빌드와 Modulith 경계 검증(`ModularityTests`)을 포함한다.
- 통합 테스트(Testcontainers)는 Docker가 없으면 **자동 스킵**된다 — 테스트 통과가 통합 검증 완료를 의미하지 않을 수 있으니, 전체 검증은 Docker 가용 환경에서 한다.
