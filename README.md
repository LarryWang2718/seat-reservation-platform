# Resilient Ticketing Platform

A Spring Boot + GraphQL + PostgreSQL backend focused on the hard parts of seat reservation: same-seat contention, transactional hold/confirm flows, replayable projections, expiration, recovery, and operational visibility.

## What This Solves

The difficult part of ticketing is not rendering a seat map. It is preserving correctness when many clients try to reserve the same inventory at the same time while the system is also emitting events, updating read models, expiring stale holds, and recovering from failed background work.

This project is built around that problem.

Core guarantees:
- one seat cannot be sold twice
- one seat cannot have more than one active hold at a time
- business writes and outbox events commit atomically
- projection tables can be rebuilt from the outbox log
- failed projection events are retried and eventually dead-lettered instead of being silently skipped

## Stack

Application:
- Java 21
- Spring Boot 3.5
- Spring GraphQL
- Spring Data JPA
- Flyway
- Micrometer / Actuator
- Lombok

Data:
- PostgreSQL 17

Testing:
- JUnit 5
- Mockito
- Spring Boot integration tests

## System Design

API layer:
- GraphQL endpoint for event, order, hold, checkout, and projection-backed read flows

Write side:
- transactional tables: `event`, `seat`, `hold`, `orders`, `ticket`
- correctness enforced through transactional services plus PostgreSQL constraints
- `outbox_event` written in the same transaction as hold creation and order confirmation

Read side:
- `seat_availability_projection` for seat-map reads
- `user_ticket_projection` for session-based ticket reads
- checkpointed projection consumer reading ordered outbox rows

Background processing:
- scheduled projection consumer
- scheduled hold expiration producer
- scheduled cancelled-order cleanup producer

Operational support:
- structured logs around hold, checkout, projection, expiration, and cleanup flows
- Micrometer counters and gauges for hold attempts/failures, checkout results, projection lag, and DLQ count
- GraphQL exception mapping for stable client-facing errors

## Database Structure

Core transactional tables:
- `event`: event metadata and sale window
- `seat`: seat inventory scoped to one event
- `hold`: temporary reservations with `HELD`, `CONFIRMED`, `EXPIRED`, `CANCELLED`
- `orders`: session-scoped checkout intent and lifecycle state
- `ticket`: final sold-seat record

Async and recovery tables:
- `outbox_event`: append-only application event log
- `projection_checkpoint`: last processed outbox id per consumer
- `projection_event_failure`: retry tracking for failed projection events
- `projection_dead_letter`: poison-event storage after retry exhaustion

Projection tables:
- `seat_availability_projection`: projection-backed seat state (`AVAILABLE`, `HELD`, `SOLD`)
- `user_ticket_projection`: query-friendly ticket view by session

Important database constraints:
- unique seat identity within one event
- unique active hold per seat
- unique pending order per session and event
- unique ticket per seat

## Key Challenges Addressed

### Same-seat contention
Multiple orders can race for the same seat, but only one active hold and one final ticket are allowed. PostgreSQL constraints are the last line of defense, and the service layer translates those violations into domain behavior.

### Business write plus async propagation
Creating a hold or confirming an order must not commit business state without its corresponding domain event. The transactional outbox solves that by writing `outbox_event` in the same transaction.

### Eventually consistent reads without losing recovery
Projection-backed reads are fast and simple, but they are not the source of truth. The system keeps them rebuildable by consuming the ordered outbox log and tracking consumer checkpoints.

### Background failure handling
Projection failures are retried, then dead-lettered after the configured limit. Hold expiration and cancelled-order cleanup both run in bounded batches so recovery work does not become an unbounded background task.

## Main Flows

### 1. Order creation
A session opens a pending order for one event. The pending-order uniqueness rule prevents multiple active checkouts for the same session and event.

### 2. Hold creation
A client requests a hold on a seat. The service validates seat ownership, seat status, order state, and the max-seat limit. The hold row and `HOLD_CREATED` outbox event commit together.

### 3. Order confirmation
Held seats are converted to tickets in one transaction. The order becomes `COMPLETED`, holds become `CONFIRMED`, and `ORDER_COMPLETED` plus `TICKET_ISSUED` outbox rows are written atomically.

### 4. Projection catch-up
A scheduled consumer reads `outbox_event` in ascending id order, updates projection tables, and advances a checkpoint. Failed events are retried and then dead-lettered.

### 5. Hold expiration and cancellation cleanup
Stale holds are expired in batches and emit `HOLD_EXPIRED`. Old cancelled orders are cleaned in batches after release events are emitted so projections stay consistent.

## Results

Local observed result:
- the projection consumer applied 500 `HOLD_CREATED` seat-state updates for one event in `765.81 ms`, which is about `652.90 seat updates/sec`

Correctness evidence in the repository:
- `HoldConcurrencyTest`: same-seat hold contention results in exactly one held seat
- `OrderConcurrencyTest`: concurrent confirmations for the same held seat result in exactly one ticket
- `OutboxIntegrationTest`: business rows and outbox rows commit or roll back together
- `ProjectionConsumerServiceTest`: retry, DLQ, replay, and checkpoint behavior
- `HoldExpirationIntegrationTest`: expired holds return seats to `AVAILABLE`
- `GraphQlExceptionResolverTest`: stable GraphQL error mapping for business and infrastructure failures

## Running Locally

Prerequisites:
- Java 21
- PostgreSQL
- a local database password exported as `DB_PASSWORD`

Start the application:

```powershell
$env:DB_PASSWORD='<local password>'
.\mvnw.cmd spring-boot:run
```

Run focused verification:

```powershell
$env:DB_PASSWORD='<local password>'
.\mvnw.cmd "-Dtest=HoldConcurrencyTest,OrderConcurrencyTest" test
.\mvnw.cmd "-Dtest=HoldExpirationIntegrationTest" test
```

## Notes

- Projection-backed reads are eventually consistent; correctness lives in the transactional tables.
- The design focuses on reservation consistency, not payment orchestration.
- The outbox is the application event log used for replay and async propagation; it is separate from PostgreSQL's internal WAL.
