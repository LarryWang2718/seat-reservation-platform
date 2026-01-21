# Phase 9 Hardening Scenarios

## GraphQL Error Handling

The GraphQL layer maps domain exceptions to stable client-facing error classes in `GraphQlExceptionResolver`.

Expected client errors are returned without stack traces:
- `NOT_FOUND` for missing event, order, or seat lookups
- `BAD_REQUEST` for checkout/hold validation failures, sale-window violations, session validation, and constraint/validation errors

Unexpected server failures are sanitized to `Internal server error`.
`OrderCleanupFailedException` is the one intentional internal error that keeps its explicit message because the caller needs to know the checkout failed after cleanup also failed.

## On-Sale Load-Test Scenario

Goal:
- Measure how the system behaves when many clients compete for a small seat pool during the first minutes of an on-sale.

Recommended shape:
- 1 event
- 500 to 2,000 seats
- 5,000 to 20,000 virtual users over 5 to 10 minutes
- 80% read traffic on `seatsByEvent`
- 15% `createOrder`
- 5% `createHold` and `confirmOrder`

Traffic mix:
1. Read the seat map.
2. Create an order for one session.
3. Attempt to hold one or more seats.
4. Confirm a subset of held orders.
5. Let some held orders expire naturally to exercise the expiration and projection pipeline.

Success criteria:
- No duplicate tickets for one seat
- No more than one active hold for one seat at a time
- Projection lag stays bounded and recovers after the spike
- DLQ count remains zero for healthy runs
- p95 GraphQL latency remains within the target budget you set for the demo

Metrics/logs to watch:
- `seat_reserve.hold.attempts`
- `seat_reserve.hold.failures`
- `seat_reserve.checkout.completed`
- `seat_reserve.projection.lag.events`
- `seat_reserve.projection.dead_letter.count`
- structured logs from hold, checkout, projection, expiration, and cleanup services

## Same-Seat Contention Scenario

The repository already has executable same-seat contention coverage:
- `HoldConcurrencyTest.concurrentHoldsOnSameSeatResultInExactlyOneHeld`
- `OrderConcurrencyTest.concurrentConfirmationsForSameHeldSeatCreateExactlyOneTicket`

Run them with:

```powershell
$env:DB_PASSWORD='<local password>'
.\mvnw.cmd "-Dtest=HoldConcurrencyTest,OrderConcurrencyTest" test
```

Expected outcome:
- exactly one successful hold when multiple orders race for the same seat
- exactly one ticket when multiple confirmations race for the same held seat
- all losing requests fail with business exceptions instead of creating corrupt state

## Zero Double-Sell Guarantee And Limits

Current guarantee:
- A seat cannot end up with more than one persisted ticket because ticket writes are protected by the database uniqueness rule and the confirmation transaction.
- A seat cannot have more than one active hold because hold creation is backed by the active-hold uniqueness rule.
- Outbox writes happen in the same transaction as hold creation and order confirmation, so business state and event-log state do not diverge on commit.

Operational limits:
- Projection-backed reads are eventually consistent; correctness lives in the transactional tables, not in the projections.
- The system still relies on database constraints rather than explicit seat-row locking during hold creation.
- There is no payment side effect in checkout, so the consistency story covers reservation state only.
- Scheduled consumers and cleanup jobs must keep running; if they stop, writes remain correct but read models and expired-seat release behavior can lag.

Evidence in this repository:
- `HoldConcurrencyTest`
- `OrderConcurrencyTest`
- `OutboxIntegrationTest`
- `HoldExpirationIntegrationTest`
- `ProjectionConsumerServiceTest`
