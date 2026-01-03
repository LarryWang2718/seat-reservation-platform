WITH ranked_pending_orders AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY session_id, event_id
               ORDER BY created_at, id
           ) AS pending_rank
    FROM orders
    WHERE status = 'PENDING'
),
duplicate_pending_orders AS (
    SELECT id
    FROM ranked_pending_orders
    WHERE pending_rank > 1
)
UPDATE hold
SET status = 'CANCELLED'
WHERE order_id IN (SELECT id FROM duplicate_pending_orders)
  AND status <> 'CONFIRMED';

WITH ranked_pending_orders AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY session_id, event_id
               ORDER BY created_at, id
           ) AS pending_rank
    FROM orders
    WHERE status = 'PENDING'
)
UPDATE orders
SET status = 'CANCELLED'
WHERE id IN (
    SELECT id
    FROM ranked_pending_orders
    WHERE pending_rank > 1
);

CREATE UNIQUE INDEX uq_order_pending_session_event
    ON orders (session_id, event_id)
    WHERE status = 'PENDING';
