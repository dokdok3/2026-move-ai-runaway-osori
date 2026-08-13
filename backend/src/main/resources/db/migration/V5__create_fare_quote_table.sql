CREATE TABLE fare_quote (
    id                  BIGSERIAL PRIMARY KEY,
    quote_key           VARCHAR(150) NOT NULL UNIQUE,
    average_fare        INTEGER NOT NULL,
    same_day_threshold  INTEGER NOT NULL,
    distance_km         INTEGER,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);
