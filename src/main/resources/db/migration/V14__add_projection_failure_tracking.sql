CREATE TABLE projection_event_failure (
    id BIGSERIAL PRIMARY KEY,
    consumer_name VARCHAR(100) NOT NULL,
    outbox_event_id BIGINT NOT NULL,
    attempt_count INT NOT NULL,
    last_error_message TEXT NOT NULL,
    first_failed_at TIMESTAMP NOT NULL,
    last_failed_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_projection_event_failure_consumer_event
        UNIQUE (consumer_name, outbox_event_id),
    CONSTRAINT fk_projection_event_failure_outbox_event
        FOREIGN KEY (outbox_event_id) REFERENCES outbox_event(id)
);

CREATE INDEX idx_projection_event_failure_consumer_event
    ON projection_event_failure (consumer_name, outbox_event_id);

CREATE TABLE projection_dead_letter (
    id BIGSERIAL PRIMARY KEY,
    consumer_name VARCHAR(100) NOT NULL,
    outbox_event_id BIGINT NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    attempt_count INT NOT NULL,
    failure_reason TEXT NOT NULL,
    first_failed_at TIMESTAMP NOT NULL,
    dead_lettered_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_projection_dead_letter_consumer_event
        UNIQUE (consumer_name, outbox_event_id),
    CONSTRAINT fk_projection_dead_letter_outbox_event
        FOREIGN KEY (outbox_event_id) REFERENCES outbox_event(id)
);

CREATE INDEX idx_projection_dead_letter_consumer_dead_lettered_at
    ON projection_dead_letter (consumer_name, dead_lettered_at, outbox_event_id);
