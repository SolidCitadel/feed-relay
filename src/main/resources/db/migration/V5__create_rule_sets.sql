-- rules 소유 (§5) — Template을 복제한 사용자 소유 규칙 묶음 (§12 규칙 계열)
CREATE TABLE rule_sets (
    id                      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                 bigint      NOT NULL REFERENCES users (id),
    origin_template_key     varchar(64) NOT NULL,
    origin_template_version int         NOT NULL,
    definition_json         text        NOT NULL,
    created_at              timestamptz NOT NULL
);
