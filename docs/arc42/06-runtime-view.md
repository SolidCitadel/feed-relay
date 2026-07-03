# 6. 런타임 뷰

## 6.1 구독 실행 (핵심 시나리오)

```
[스케줄러: 매분 tick — adapter/in/scheduler]
  SELECT … FROM subscriptions WHERE next_run_at <= now AND status='ACTIVE'
  FOR UPDATE SKIP LOCKED            -- Postgres 잡큐, 멀티 인스턴스 안전
  → next_run_at = now + 30분 + jitter(0~5분)

[RunSubscriptionUseCase — sync가 오케스트레이션, 구독 단위 실패 격리]
  1. fetch      : ingestion — ical GET (타임아웃 10s, ETag 활용) → List<SourceItem>
  2. evaluate   : rules — first-match-wins → Route(slot, OutboundItem) | Exclude
  3. snapshot   : delivery — 대상 리스트 상태 전수 조회 (리스트당 1콜)
                  → 완료 감지 FROZEN(COMPLETED), 소실 감지 FROZEN(DELETED)
  4. diff       : sync — SyncMapping·content_hash 대조 → creates/updates/skips
  5. apply      : delivery.create/update — 항목 단위 실패는 기록 후 계속(PARTIAL)
  6. record     : RunLog + last_run_at → 커밋 후 RunCompleted/RunFailed 발행
```

- 오류 정책: 피드·API 연속 3회 실패 → 구독 status=ERROR + RunFailed. 429/5xx 지수 백오프.
- 피드에서 항목이 창 밖으로 사라진 경우: 무시 (삭제 동기화 없음 — ADR-0003과 일관).

## 6.2 온보딩 (React가 플로우 조립 — 각 단계 = 한 모듈)

```
가입      : Google OAuth(openid,email,profile) → identity: users upsert(google_sub 기준, 프로필은 매 로그인 동기화) → 세션
소스 등록 : ical URL → ingestion: fetch·검증 → 항목 미리보기
템플릿    : rules: Canvas 템플릿 시험 적용 → 과목(slot 후보) 자동 추출
대상 연결 : connections: incremental consent(scope=tasks, access_type=offline)
슬롯 매핑 : 기존 리스트 선택 or 생성(tasklists.insert) → sync: 구독 생성
첫 실행   : 즉시 트리거 → 결과 요약("47개 중 32개 등록, 15개 제외")
```

## 6.3 위임 철회 (통지 평면의 역방향 시나리오)

```
connections: refresh 실패로 철회 감지 → Connection status=REVOKED
  ⇢ ConnectionRevoked 발행 (커밋 후 비동기, at-least-once)
sync: 리스너가 해당 구독들 status=ERROR ⇢ RunFailed 경로로 운영자 알림
```
