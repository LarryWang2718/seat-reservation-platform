# Resilient Ticketing Platform Development Schedule

Use this file as the working checklist for the project. At the end of each day, mark completed items with `[x]` and leave unfinished items as `[ ]`.

## Phase 1: Foundation and event model

- [x] Define package structure for `event`, `seat`, `hold`, `order`, `ticket`
- [ ] Add package structure for `payment`
- [x] Add package structure for `outbox`
- [x] Implement `EventStatus`
- [x] Implement `Event` JPA entity
- [x] Implement `EventRepository`
- [x] Create DTOs for event create/list APIs
- [ ] Implement `POST /events`
- [ ] Implement `GET /events`
- [x] Implement GraphQL event create/list flow
- [ ] Add tests for event creation and listing

## Phase 2: Seat inventory model

- [x] Design assigned-seat schema for one event
- [x] Create Flyway migration for `seat`
- [x] Implement `Seat` entity
- [x] Implement `SeatRepository`
- [x] Enforce unique seat identity within an event
- [x] Add seat creation/bootstrap path for an event
- [x] Implement GraphQL seat create/list flow
- [ ] Add tests for seat persistence and uniqueness rules

## Phase 3: Hold flow and concurrency boundary

- [x] Design `HoldStatus` values
- [x] Create Flyway migration for `hold`
- [x] Implement `Hold` entity
- [x] Implement `HoldRepository`
- [x] Add DB constraint/index for one active hold per seat
- [x] Add service method to start checkout hold
- [x] Enforce max 4 seats per checkout
- [x] Enforce one active checkout per user per event
- [ ] Lock seat rows transactionally during hold creation
- [x] Add tests for successful hold creation
- [x] Add concurrent tests proving no duplicate active hold on one seat
- [x] Implement GraphQL hold create/list flow

## Phase 4: Order, ticket, and checkout completion

- [x] Design `OrderStatus`
- [ ] Design `TicketStatus`, `PaymentStatus`
- [x] Create Flyway migration for `order`
- [x] Create Flyway migration for `ticket`
- [ ] Create Flyway migration for `payment`
- [x] Implement `Order` entity and repository
- [x] Implement `Ticket` entity and repository
- [ ] Implement `Payment` entity and repository
- [x] Add DB rule preventing double-sell of a seat
- [x] Implement checkout confirmation transaction
- [x] Convert valid holds into sold tickets atomically
- [x] Add tests for successful checkout
- [x] Add concurrent tests proving a seat cannot be sold twice

## Phase 5: Transactional outbox / write-ahead log

- [x] Design `outbox_event` schema
- [x] Create Flyway migration for `outbox_event`
- [x] Implement `OutboxEvent` entity and repository
- [x] Emit outbox records in the same transaction as hold creation
- [x] Emit outbox records in the same transaction as checkout completion
- [x] Define event payloads for hold created, hold expired, order completed, ticket issued
- [x] Add tests proving business write and outbox write commit together

## Phase 6: Projection pipeline

- [x] Design projection checkpoint strategy
- [x] Create projection table for seat availability
- [x] Create projection table for user tickets
- [x] Implement ordered projection consumer
- [x] Make projection consumer idempotent
- [x] Store last processed event/checkpoint
- [ ] Add replay capability from outbox/event log
- [ ] Add tests for replay rebuilding seat availability
- [ ] Add tests for replay rebuilding user ticket view

Blockers / follow-ups:
- [ ] Implement `HOLD_EXPIRED` projection handling before enabling the hold-expiration producer
- [ ] Switch GraphQL seat reads to `seat_availability_projection`
- [ ] Add a GraphQL/user read path backed by `user_ticket_projection`

## Phase 7: Resilience features

- [ ] Add retry policy for failed projection processing
- [ ] Add DLQ table for poison events
- [ ] Record failure reason and attempt count
- [ ] Add scheduled job to expire stale holds
- [ ] Process hold expiration in batches
- [ ] Add batched cleanup for cancellations
- [ ] Add tests for retry and DLQ behavior
- [ ] Add tests for automatic hold expiration

## Phase 8: GraphQL API

- [x] Define GraphQL schema for events
- [x] Define GraphQL schema for seat map
- [x] Define GraphQL mutations for hold and checkout
- [ ] Resolve reads from projection tables
- [x] Route writes through transactional services
- [x] Add GraphQL integration tests

## Phase 9: Hardening and observability

- [ ] Add structured logging around hold, checkout, projection, and expiration flows
- [ ] Add metrics for hold attempts, hold failures, checkout success, projection lag, DLQ count
- [ ] Add error handling for API and GraphQL layers
- [ ] Add load-test scenario for on-sale traffic
- [ ] Add concurrency test scenario for same-seat contention
- [ ] Document zero double-sell guarantee and limits

## Phase 10: Resume-grade finish

- [ ] Record the final architecture decisions
- [ ] Document the transactional write model
- [ ] Document the replay/recovery model
- [ ] Document projection ordering and idempotency strategy
- [ ] Capture benchmark numbers for seat updates per event
- [ ] Capture evidence of retry, DLQ, and backfill behavior
- [ ] Write a short project summary aligned to resume bullets

## Suggested daily cadence

- [ ] Pick 1-3 checklist items before starting
- [ ] Finish code + tests for those items only
- [ ] Update this file before stopping for the day
- [ ] Note blockers directly under the phase you are in
