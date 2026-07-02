# 3. 컨텍스트와 범위

```
사용자(브라우저) ──HTTPS──▶ FeedRelay ──pull(ics)──▶ Canvas LMS ical 피드
                              │
                              ├──OAuth/REST──▶ Google (로그인 · Tasks API)
                              └──webhook────▶ Discord (운영자 알림)
```

- **소유하지 않은 외부 언어 경계 2곳**: ical 세계(VEVENT, UID) ↔ ingestion이 번역 / Google Tasks 세계(TaskList, Task) ↔ delivery가 번역. 판정 기준은 [§12](12-glossary.md), 경위는 [ADR-0004](../adr/0004-single-bounded-context.md).
- 범위 밖(v1): 웹훅·스크래핑 소스(포트만 설계), Todoist·Google Calendar 대상, 사용자별 알림, 정규식 편집기.
