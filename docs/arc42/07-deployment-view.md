# 7. 배포 뷰

## 로컬 개발

- `docker compose up -d postgres` + `./gradlew bootRun` (frontend 빌드 포함, 단일 오리진)
- 프론트 개발 서버: `frontend/ npm run dev` → :5173, `/api`·`/actuator`·`/oauth2`·`/login` 프록시 → :8080
- 전체 검증: `./gradlew bootJar && docker compose up --build` → app 헬스체크

## 프로덕션 목표 (M5)

```
Oracle Cloud Always Free (A1 Flex)
├── 엣지 compose (infra/) : Caddy — :80/:443 점유, 인증서 볼륨, 외부 네트워크 `edge` 제공
└── 앱 compose            : app(boot jar) + postgres(볼륨) — `edge` 참여
```

- 분리 근거: 수명주기 불일치 — [ADR-0010](../adr/0010-edge-compose-separation.md). k3s는 2단계 마이그레이션 — [ADR-0001](../adr/0001-modular-monolith-and-compose.md).
- 고정 도메인 + HTTPS 필수(OAuth 리다이렉트). 도메인 전략·리스크는 §11.
- 백업: 야간 pg_dump → Oracle Object Storage(오프박스). 알림: Discord webhook.

## CI/CD

- GitHub Actions: setup-node + setup-java 21 → `./gradlew build` (Testcontainers 통합 테스트 포함)
- 배포(M5): docker build → GHCR push → SSH `docker compose pull && up -d`
