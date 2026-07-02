# 2. 제약

| 제약 | 내용 |
|---|---|
| 인력 | 솔로 개발·운영 |
| 인프라 비용 | 0원 목표 — Oracle Cloud Always Free(A1 Flex). 용량 부족 빈발·유휴 회수 정책 있음 (§11) |
| Google OAuth | 미검증 프로덕션 앱: 100명 캡 + 경고 화면. 테스트 모드 금지(refresh token 7일 만료 — 동기화 서비스에 치명) |
| 도메인 | 무료 도메인으로 시작 — PSL 미등재 서비스(kro.kr)는 OAuth 승인 도메인·LE 발급 한도 리스크 (§11, [ADR-0002](../adr/0002-name-feedrelay.md)) |
| 소스 | LMS는 쓰기 불가 → 단방향 동기화가 구조적으로 강제됨 |
| 대상 | Google Tasks due는 날짜만 지원(시각 무시) — 어댑터가 흡수 (§8) |
