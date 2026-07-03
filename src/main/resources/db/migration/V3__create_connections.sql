-- connections 소유 (§5) — 위임 그랜트: 토큰 암호화 보관 (§8.5)
CREATE TABLE connections (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           bigint       NOT NULL REFERENCES users (id),
    provider          varchar(32)  NOT NULL,
    scopes            varchar(512) NOT NULL,
    access_token_enc  text,
    refresh_token_enc text         NOT NULL,
    expires_at        timestamptz,
    status            varchar(16)  NOT NULL,
    created_at        timestamptz  NOT NULL,
    UNIQUE (user_id, provider)
);
