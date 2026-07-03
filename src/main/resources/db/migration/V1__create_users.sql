-- identity 소유 (§5) — PK 전략은 ADR-0013 (bigint identity)
CREATE TABLE users (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    google_sub   varchar(255) NOT NULL UNIQUE,
    email        varchar(320) NOT NULL,
    display_name varchar(255),
    created_at   timestamptz  NOT NULL
);
