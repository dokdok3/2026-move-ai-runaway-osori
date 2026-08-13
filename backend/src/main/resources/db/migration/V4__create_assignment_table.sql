CREATE TABLE assignment (
    id           BIGSERIAL PRIMARY KEY,
    cargo_id     BIGINT NOT NULL UNIQUE,
    driver_id    BIGINT NOT NULL,
    accepted_at  TIMESTAMP NOT NULL
);
