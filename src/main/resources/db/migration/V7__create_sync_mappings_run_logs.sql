-- sync 소유 (§5) — 항목 단위 동기화 상태(멱등성의 단위)와 실행 이력 (ADR-0003)
CREATE TABLE sync_mappings (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id bigint       NOT NULL REFERENCES subscriptions (id),
    source_uid      varchar(512) NOT NULL,
    external_ref    varchar(512) NOT NULL,
    content_hash    varchar(64)  NOT NULL,
    status          varchar(16)  NOT NULL,
    frozen_reason   varchar(16),
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    UNIQUE (subscription_id, source_uid)
);

CREATE TABLE run_logs (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscription_id bigint      NOT NULL REFERENCES subscriptions (id),
    started_at      timestamptz NOT NULL,
    finished_at     timestamptz NOT NULL,
    result          varchar(16) NOT NULL,
    stats_json      text        NOT NULL,
    error_summary   text
);
