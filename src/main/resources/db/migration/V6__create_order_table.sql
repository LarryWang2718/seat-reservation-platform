CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_event
        FOREIGN KEY (event_id) REFERENCES event(id)
);
