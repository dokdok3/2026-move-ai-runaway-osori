CREATE TABLE driver (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(50) NOT NULL,
    phone_number          VARCHAR(20),
    plate_number          VARCHAR(20),
    vehicle_type          VARCHAR(20),
    capacity_ton          NUMERIC(6,1),
    body_type             VARCHAR(20),
    vehicle_cargo_types   VARCHAR(100),
    rating                NUMERIC(3,1),
    total_trips           INTEGER,
    completion_rate       INTEGER,
    min_accept_fare       INTEGER,
    contactable_from      VARCHAR(5),
    contactable_to        VARCHAR(5),
    recent_trip_summary   VARCHAR(100),
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE driver_route_preference (
    id         BIGSERIAL PRIMARY KEY,
    driver_id  BIGINT NOT NULL,
    direction  VARCHAR(20) NOT NULL,
    sido       VARCHAR(20) NOT NULL,
    sigungu    VARCHAR(20)
);
