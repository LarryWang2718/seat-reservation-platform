CREATE TABLE seat (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    section VARCHAR(100) NOT NULL,
    row_label VARCHAR(50) NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    CONSTRAINT fk_seat_event
        FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT uq_seat_event_identity
        UNIQUE (event_id, section, row_label, seat_number)
);
