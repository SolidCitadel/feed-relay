# ADR-0005: 계약 타입 생산자 소유 — shared 패키지 폐기, 사실/의도 분리

- 날짜: 2026-07-02 / 상태: Accepted

## 맥락

초기 설계는 `shared` 패키지(공유 커널→Published Language로 재명명)에 SourceItem을 두었다. 정당화는 "rules도 SourceItem을 생산(변환 산출물)하므로 소유가 모호하다"였으나, 모델을 순방향으로 재열거하자 "소스가 말한 **사실**"과 "대상에 쓰려는 **의도**"가 다른 의미임이 드러남 — 같은 타입의 재사용은 유비쿼터스 언어 위반이고, 변환 전 값을 해싱·배달하는 버그가 타입에서 안 잡힌다.

## 검토한 대안

1. shared 유지(단일 SourceItem): 사실/의도 혼용 — 기각
2. 컨텍스트별 모델 + 경계마다 번역: 언어가 갈라지지 않는 개념에 가짜 경계(내용 없는 번역) — 기각
3. **사실/의도 타입 분리 + 생산자 소유** (채택)

## 결정

`SourceItem`(사실)은 ingestion.api, `OutboundItem`(의도)은 rules.api 소유. shared 패키지는 폐기 — 각 계약의 생산자가 유일해져 존재 근거가 소멸. 하류는 타입만 conformist 의존: rules →(SourceItem)→ ingestion / delivery →(OutboundItem)→ rules, 파이프라인 방향으로만. content_hash·delivery의 대상은 항상 OutboundItem.

## 결과

- 단일 컨텍스트(ADR-0004) 안이므로 Shared Kernel/Published Language 같은 컨텍스트 간 패턴 어휘 자체가 부적용 — 관련 라벨 폐기.
- 단일 컨텍스트의 언어 안에서도 "사실/의도" 같은 의미 분화는 타입으로 강제한다.
