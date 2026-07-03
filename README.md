# FeedRelay

> LMS가 ical 피드로만 내보내는 과제를, 과목별로 분류된 **체크 가능한 할 일**로 바꿔주는 서비스.
> 다양한 소스(v1: ical)의 항목을 정규식 규칙(템플릿)으로 분류·변환해 캘린더/Todo 앱(v1: Google Tasks)에 단방향 동기화한다.

🚧 **개발 중** — M3까지 완료(로그인·위임·파이프라인 수동 실행), 다음은 M4(스케줄러·온보딩 마법사) · 로드맵: [arc42 §1](docs/arc42/01-introduction-and-goals.md)

## 왜 만드나

Canvas 등 LMS는 과제를 ical 캘린더 피드로 제공하지만:

- Google Calendar 구독은 갱신이 느리고(최대 24시간), 모든 과목이 한 캘린더에 섞이며, **체크할 수 없는 일정**으로만 표시된다
- Todoist·Google Tasks에는 ical을 넣는 경로 자체가 없다
- Zapier/Make 같은 범용 자동화 도구는 학생이 쓰기엔 진입장벽이 높다

FeedRelay는 **템플릿 선택 → 과목→리스트 매핑 → 완료** 세 단계로 이걸 해결한다. 과제는 과목별 리스트에 할 일로 들어오고, 마감일 변경은 따라오며(단 한 번씩만 만들고 바뀐 만큼만 고침), 사용자가 완료·삭제한 항목은 다시 건드리지 않는다.

## 아키텍처 한눈에

Kotlin · Spring Boot(Modulith) · React(Vite) · PostgreSQL · Docker Compose

- **모듈러 모놀리스** — 단일 바운더리 컨텍스트 안의 서브도메인 정렬 모듈 7개, 경계는 Modulith 테스트로 기계 강제
- **전 모듈 동일한 정식형 헥사고날** 내부 구조 — 계약 타입은 생산자 모듈이 소유
- **통신 2평면** — 파이프라인(응답 필요)은 오케스트레이션 직접 호출, 통지(사실)는 애플리케이션 이벤트
- **React 동일 오리진 서빙** — Vite 산출물을 boot jar에 동봉, 세션 쿠키 인증 유지

상세: [arc42 §4 해법 전략](docs/arc42/04-solution-strategy.md) · [§5 빌딩 블록](docs/arc42/05-building-blocks.md)

## 시작하기

요구사항: **JDK 21 · Node 24+ · Docker**(통합 테스트·전체 기동 시)

```bash
cp .env.example .env            # Google OAuth 클라이언트·토큰 암호화 키 채우기 (파일 내 안내)
docker compose up -d postgres   # DB 기동
./gradlew bootRun               # 백엔드 + 프론트 (동일 오리진, :8080)
```

개발·검증:

```bash
cd frontend && npm run dev                        # 프론트 개발 서버 (:5173 → :8080 프록시)
./gradlew build                                   # 빌드 + 테스트 (Modulith 경계 검증 포함, 통합 테스트는 Docker 필요 — 없으면 자동 스킵)
./gradlew bootJar && docker compose up --build    # 전체 기동 검증 → http://localhost:8080/actuator/health
```

## 문서

| 위치 | 내용 |
|---|---|
| [docs/arc42/](docs/arc42/) | 아키텍처 문서 (arc42 골격) — [§1](docs/arc42/01-introduction-and-goals.md) 목표·제품 정책·로드맵 · [§5](docs/arc42/05-building-blocks.md) 모듈 구조 · [§6](docs/arc42/06-runtime-view.md) 런타임 · [§8](docs/arc42/08-crosscutting-concepts.md) 도메인 모델·보안 · [§10](docs/arc42/10-quality-requirements.md) 품질 시나리오 · [§11](docs/arc42/11-risks-and-technical-debt.md) 리스크 · [§12](docs/arc42/12-glossary.md) 용어 |
| [docs/adr/](docs/adr/) | 아키텍처 결정 기록(ADR) — 각 결정의 맥락·검토한 대안·근거·재검토 트리거 |

## 라이선스

[MIT](LICENSE)
