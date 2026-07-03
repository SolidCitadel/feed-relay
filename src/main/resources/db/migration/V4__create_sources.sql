-- ingestion 소유 (§5) — 사용자가 등록한 수집처
CREATE TABLE sources (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     bigint       NOT NULL REFERENCES users (id),
    type        varchar(32)  NOT NULL,
    name        varchar(255) NOT NULL,
    config_json text         NOT NULL,
    status      varchar(16)  NOT NULL,
    created_at  timestamptz  NOT NULL
);
