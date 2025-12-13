CREATE TABLE hold (
    id BIGSERIAL PRIMARY KEY,
    seat_id BIGINT NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hold_seat
        FOREIGN KEY (seat_id) REFERENCES seat(id)
);

CREATE UNIQUE INDEX uq_hold_active_seat
    ON hold (seat_id)
    WHERE status = 'HELD';
