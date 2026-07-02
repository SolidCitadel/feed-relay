# ADR-0002: 이름 FeedRelay (Relay 단독 폐기)

- 날짜: 2026-07-02 / 상태: Accepted

## 맥락

초안 "Relay"는 은유(받아서 전달)는 정확하나, relay.app(동일 카테고리 워크플로 자동화, a16z/Khosla 투자 ~$35M)이 "relay + {Todoist, Google Tasks…앱이름}" 검색 지형을 프로그래매틱 SEO로 선점 — 우리 서비스명+기능 검색 결과가 경쟁사 랜딩이 되는 구조. Meta의 GraphQL Relay 등 일반명사 오염도 심각. 성장을 열어둔 이상 개명 비용은 초기가 최저점.

## 검토한 대안

{feed, task, todo, schedule} × {sync, relay} 매트릭스 전수 평가. 축: ① 대상 박제 여부(todo/schedule은 목적지 절반을 이름에 고정 — 확장과 충돌) ② relay 간섭 비용 ③ 도메인 가용성.

- FeedSync: 대상 중립·간섭 0이나 sync는 양방향 뉘앙스 + 죽은 MS 프로토콜 동명 + 몰개성(sync 조합 도메인 대부분 선점이 방증)
- TodoRelay/ScheduleRelay: 이해도 최고이나 대상 박제
- 실단어(Baton, Chute, Switchyard 등): .app 전부 선점

## 결정

**FeedRelay**. 근거: relay(중계)가 단방향 생성+수정 전달이라는 동기화 의미론과 정확히 일치(sync보다 정밀), feed는 소스 서술이라 대상 중립, 동명 제품 없음, relay.app SEO 패턴("relay+앱이름")의 밖, 원안 정체성 계승. 도메인은 무료로 시작하되 kro.kr은 PSL 미등재(OAuth 승인 도메인·LE 한도 리스크) — PSL 등재 무료 대안 duckdns.org 우선, 막히면 저가 유료.

## 결과

- 단독 "Relay"류 명명 재사용 금지.
- 재검토 트리거: 유료 도메인 구매 시점(공개 전).
