# ADR-0008: UI는 React SPA + Spring 동일 오리진 정적 서빙

- 날짜: 2026-07-02 / 상태: Accepted

## 맥락

v1 UI는 온보딩 플로우(폼 4~5장 + OAuth 리다이렉트). v2에 정규식 편집기(실시간 프리뷰 등 rich UI) 가능성. 사용자는 React 생태계 선호.

## 검토한 대안

1. Thymeleaf+htmx: 배포 1단위·세션 자연스러움. 단 rich UI로 갈수록 한계, 사용자 숙련도와 불일치. (렌더링 방식과 디자인 표현력은 독립 — 기각 사유는 상호작용 복잡도와 숙련도)
2. React/Next 완전 분리: CORS·토큰 저장 설계·배포 2단위 비용만 추가 — 기각
3. **React SPA를 Spring이 동일 오리진에서 정적 서빙** (채택)

## 결정

Vite 빌드 산출물을 Gradle `processResources`로 boot jar `static/`에 동봉. 세션 쿠키 인증 유지(CORS/토큰 설계 통째로 회피), OAuth 리다이렉트 자연스러움, 배포 1단위. 개발은 Vite 프록시(:5173→:8080). 인증은 세션 쿠키(host-only+Secure+SameSite=Lax) + Spring Security CSRF 쿠키 방식.

## 결과

- v2 편집기도 재작성 없이 확장.
- 온보딩 플로우 조립은 React가 담당(각 단계가 정확히 한 모듈의 오퍼레이션에 대응 — 백엔드 조립 모듈 불요).
