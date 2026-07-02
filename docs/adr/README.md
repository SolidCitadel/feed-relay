# ADR (Architecture Decision Records)

## 생성 기준 — 무엇이 ADR감인가

다음을 대체로 충족하는 **아키텍처 결정**만 기록한다 (Nygard의 "architecturally significant"):

1. 구조·의존·계약·데이터·기술 선택에 영향을 준다
2. 진지하게 비교한 대안이 실재한다
3. 되돌리기 비용이 크다
4. 미래에 "왜 이렇게 했지?"를 물을 것이다

**ADR감이 아닌 것**: 명료화, 오탈자, 세부 보강, 문서 재배치, 구현 디테일 — living 문서 직접 수정 + 커밋 메시지로 충분하다. ADR의 트리거는 서술 변경이 아니라 **결정**이다.

## 수명주기

```
Proposed ──(승인·커밋)──▶ Accepted ──(효력 후 번복)──▶ Superseded by ADR-NNNN
    │
    └─ 같은 검토 사이클 안의 수정은 본문 수정(amend) — 불변성은 Accepted부터
```

- 한 결정 = 한 파일. 번호는 재사용하지 않는다.
- Accepted 이후 본문은 고정 — 번복은 새 ADR + 기존 것에 `상태: Superseded by ADR-NNNN`만 추가.
- Supersede는 **효력을 가졌던 결정의 번복**에만 쓴다. 검토 중 퇴고에 쓰지 않는다.
- 아키텍처 결정을 동반하는 living 문서(`docs/arc42/*`) 변경은 **ADR 작성 → living 갱신** 순서로 한다.
- living 문서는 현재형 진실만 서술한다(경위·이력 서술 금지) — 맥락·대안·근거는 여기에 산다.

## 템플릿

```markdown
# ADR-NNNN: 제목
- 날짜: YYYY-MM-DD / 상태: Proposed | Accepted | Superseded by ADR-NNNN
## 맥락
## 검토한 대안
## 결정
## 결과 (트레이드오프 · 재검토 트리거)
```
