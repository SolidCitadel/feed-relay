-- 사용자 타임존 — 대상 앱의 날짜 전용 필드(예: Google Tasks due) 변환 기준 (§8.7)
ALTER TABLE users ADD COLUMN timezone varchar(64) NOT NULL DEFAULT 'Asia/Seoul';
