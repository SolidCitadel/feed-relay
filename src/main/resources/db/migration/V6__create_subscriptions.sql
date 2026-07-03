-- sync 소유 (§5) — 무엇을 어디로: Source × RuleSet × Connection × 대상의 조합 계약 (§12)
CREATE TABLE subscriptions (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           bigint      NOT NULL REFERENCES users (id),
    source_id         bigint      NOT NULL REFERENCES sources (id),
    rule_set_id       bigint      NOT NULL REFERENCES rule_sets (id),
    connection_id     bigint      NOT NULL REFERENCES connections (id),
    destination_type  varchar(32) NOT NULL,
    slot_mapping_json text        NOT NULL,
    next_run_at       timestamptz,
    status            varchar(16) NOT NULL,
    last_run_at       timestamptz,
    created_at        timestamptz NOT NULL
);
