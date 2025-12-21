ALTER TABLE hold
ADD COLUMN order_id BIGINT;

ALTER TABLE hold
ADD CONSTRAINT fk_hold_order
    FOREIGN KEY (order_id) REFERENCES orders(id);

CREATE INDEX idx_hold_order_id
    ON hold (order_id);
