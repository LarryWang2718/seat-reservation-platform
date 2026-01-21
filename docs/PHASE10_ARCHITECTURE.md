# Phase 10 Architecture Notes

## Final Architecture Decisions

This project stays a single Spring Boot application with a GraphQL API and PostgreSQL as the only durable system of record.

Key choices:
- transactional tables (`event`, `seat`, `hold`, `orders`, `ticket`) are the correctness boundary
- PostgreSQL uniqueness constraints enforce no double-sell and one active hold per seat
- `outbox_event` is the application write-ahead log for async work and replay
- projection tables are read models, not the source of truth
- background schedulers handle projection catch-up, hold expiration, and cancelled-order cleanup
- Micrometer metrics and structured logs provide operational visibility

This keeps the project focused while still showing transactional consistency, async propagation, retries, DLQ handling, and replay.

## Transactional Write Model

### Order creation
- `createOrder` validates sale-window and session rules.
- The pending-order uniqueness constraint prevents more than one active checkout per session and event.

### Hold creation
- `createHold` validates order state, seat ownership, max-seat limit, sold-seat checks, and existing active holds.
- The active-hold uniqueness constraint on seat is the final concurrency boundary.
- The hold row and `HOLD_CREATED` outbox row commit together in one transaction.

### Order confirmation
- `confirmOrder` loads confirmable held seats, creates tickets, marks holds confirmed, marks the order completed, and then writes `ORDER_COMPLETED` plus `TICKET_ISSUED` outbox rows.
- If business validation fails, the order is cancelled.
- If the outbox write fails, the transaction rolls back and the order remains retryable.

## Replay And Recovery Model

The outbox is append-only and ordered by `outbox_event.id`.

Recovery flow:
1. A projection consumer reads rows where `id > last_processed_event_id`.
2. It applies events in ascending order.
3. It advances the checkpoint only after the event is handled successfully, or after it is dead-lettered once retry attempts are exhausted.
4. If projection tables are lost or corrupted, they can be rebuilt by replaying the outbox from an earlier checkpoint.

Operational recovery features:
- retry tracking in `projection_event_failure`
- poison-event isolation in `projection_dead_letter`
- scheduled projection catch-up
- scheduled hold expiration producer
- scheduled cancellation cleanup producer

## Projection Ordering And Idempotency

Ordering strategy:
- process strictly by increasing outbox id
- maintain one checkpoint per consumer name

Idempotency strategy:
- `TICKET_ISSUED` upserts user-ticket projection rows by ticket id
- `HOLD_EXPIRED` only frees a seat when the incoming `holdId` still matches the projection, so an old expiry event cannot erase a newer hold or sold state
- replay safety relies on ordered replay from a checkpoint, not on arbitrary out-of-order event reapplication

## Evidence Of Retry, DLQ, And Backfill Behavior

The repository already contains executable evidence for the core resilience claims.

Key tests:
- `ProjectionConsumerServiceTest.processNextBatchRecordsRetryableFailureAndLeavesCheckpointBehind`
- `ProjectionConsumerServiceTest.processNextBatchDeadLettersPoisonEventAfterMaxAttemptsAndAdvancesCheckpoint`
- `ProjectionConsumerServiceTest.replayRebuildsSeatAvailabilityProjectionFromOutboxLogAcrossBatches`
- `ProjectionConsumerServiceTest.replayRebuildsUserTicketProjectionFromOutboxLog`
- `OutboxIntegrationTest.createHoldRollsBackBusinessWriteWhenOutboxWriteFails`
- `OutboxIntegrationTest.confirmOrderLeavesOrderRetryableWhenOutboxWriteFails`
- `HoldExpirationIntegrationTest.holdExpirationFlowReturnsSeatToAvailable`

These give you concrete proof points for retry, DLQ, backfill/replay, transactional outbox coupling, and expiration recovery.

