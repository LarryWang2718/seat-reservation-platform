CREATE TABLE projection_checkpoint (
    consumer_name VARCHAR(100) PRIMARY KEY,
    last_processed_event_id BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE seat_availability_projection (
    seat_id BIGINT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    section VARCHAR(100) NOT NULL,
    row_label VARCHAR(50) NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    order_id BIGINT,
    hold_id BIGINT,
    session_id VARCHAR(255),
    hold_expires_at TIMESTAMP,
    ticket_id BIGINT,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_seat_availability_projection_seat
        FOREIGN KEY (seat_id) REFERENCES seat(id),
    CONSTRAINT fk_seat_availability_projection_event
        FOREIGN KEY (event_id) REFERENCES event(id)
);

CREATE INDEX idx_seat_availability_projection_event
    ON seat_availability_projection (event_id, status, seat_id);

INSERT INTO seat_availability_projection (
    seat_id,
    event_id,
    section,
    row_label,
    seat_number,
    status,
    updated_at
)
SELECT
    seat.id,
    seat.event_id,
    seat.section,
    seat.row_label,
    seat.seat_number,
    'AVAILABLE',
    CURRENT_TIMESTAMP
FROM seat
ON CONFLICT (seat_id) DO NOTHING;

CREATE TABLE user_ticket_projection (
    ticket_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    seat_id BIGINT NOT NULL,
    section VARCHAR(100) NOT NULL,
    row_label VARCHAR(50) NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_ticket_projection_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_user_ticket_projection_event
        FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT fk_user_ticket_projection_seat
        FOREIGN KEY (seat_id) REFERENCES seat(id),
    CONSTRAINT fk_user_ticket_projection_ticket
        FOREIGN KEY (ticket_id) REFERENCES ticket(id)
);

CREATE INDEX idx_user_ticket_projection_session_event
    ON user_ticket_projection (session_id, event_id, ticket_id);
