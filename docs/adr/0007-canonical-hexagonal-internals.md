# ADR-0007: 모듈 internal은 전 모듈 동일한 정식형 헥사고날

- 날짜: 2026-07-03 / 상태: Accepted

## 맥락

초안 "복잡도 비례 계층화(평탄→커지면 승격)"는 전역 설계 규칙("일단 최소한만 만들고 문제되면 수정" 지양)과 정면 충돌하여 철회. 전 모듈 동일 구조로 합의 후, 관례 선택이 남음. 사용자 기준: 산업 표준(국제 가독성)·학습 전시.

## 검토한 대안

1. **C/Q-first**(command/{interfaces,application,domain,infra} + query/{…}): C/Q 패키지 축 자체는 국제적(Axon, .NET Vertical Slice)이나 측면별 4계층 중첩은 국내 변형에 가깝고, CQRS 기계(이벤트소싱 등) 없이 축만 쓰면 오독 위험. 애그리거트 없는 모듈(delivery, notifications)에서 공동(空洞) 구조 발생.
2. 계층형 DDD(domain/application/infra 평행): C/Q 미표현.
3. **정식형 헥사고날**(Cockburn/Hombergs buckpal 원형, 채택): 간소형(포트 생략)이 실무 다수지만 학습 전시 목적엔 원형이 부합.

## 결정

전 모듈 동일: `api`(@NamedInterface) + `internal/{domain, application/{port/in, port/out, service}, adapter/{in/…, out/…}}`.

- 구동자(사용자/시계/이벤트)는 인바운드 어댑터의 속성일 뿐 코어는 무관심 — 스케줄러 구동 유스케이스도 웹 구동과 동형("엔진" 같은 별도 범주 불요).
- CQRS는 인바운드 포트 명명으로 표현(XxxUseCase/XxxQuery — buckpal 방식). 쿼리 서비스는 domain 우회 프로젝션 허용.
- 도메인·영속 모델 겸용(domain에 JPA 허용 — Hombergs no-mapping 전략). RuleEngine 등 순수 로직은 JPA 비의존.
- 폴더는 첫 클래스와 함께 생성(빈 폴더 금지) — 규칙은 동일, 실체는 필요 시점.

## 결과

- 모듈≠애그리거트(모듈당 0..N): sync=Subscription·SyncMapping·RunLog, delivery=0 등 — 애그리거트 경계는 불변식·트랜잭션 단위로 판정.
- 재검토 트리거: 읽기 모델이 실제 분화(별도 저장소·프로젝션)하면 C/Q 패키지 축 승격 검토.
