CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_event
        FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT fk_ticket_seat
        FOREIGN KEY (seat_id) REFERENCES seat(id),
    CONSTRAINT fk_ticket_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT uq_ticket_seat
        UNIQUE (seat_id)
);
