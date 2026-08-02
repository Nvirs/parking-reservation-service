CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    is_electric             BOOLEAN NOT NULL DEFAULT FALSE,
    has_handicapped_permit  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now()
);

-- Existing reservations reference a free-text requester with no matching user
TRUNCATE TABLE reservations;

ALTER TABLE reservations DROP COLUMN requester;

ALTER TABLE reservations ADD COLUMN user_id BIGINT NOT NULL;
ALTER TABLE reservations ADD CONSTRAINT fk_reservations_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE;

-- speeds up lookups of all reservations for a given user
CREATE INDEX idx_reservations_user_id ON reservations (user_id);
