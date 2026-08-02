-- Enables gen_random_uuid() used as the default for primary keys below.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE parking_spots (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(50) NOT NULL,
    type       VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_parking_spots_code UNIQUE (code),
    CONSTRAINT ck_parking_spots_type CHECK (type IN ('STANDARD', 'EV'))
);

CREATE TABLE reservations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parking_spot_id  BIGINT NOT NULL,
    requester        VARCHAR(255) NOT NULL,
    start_time       TIMESTAMP NOT NULL,
    end_time         TIMESTAMP NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_reservations_parking_spot
        FOREIGN KEY (parking_spot_id) REFERENCES parking_spots (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_reservations_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT ck_reservations_time_range CHECK (start_time < end_time)
);

-- speeds up lookups of all reservations for a given parking spot
CREATE INDEX idx_reservations_parking_spot_id ON reservations (parking_spot_id);

-- speeds up overlap/duration queries
CREATE INDEX idx_reservations_start_end_time ON reservations (start_time, end_time);

-- conflicting reservations
CREATE INDEX idx_reservations_spot_start_end_time ON reservations (parking_spot_id, start_time, end_time);
