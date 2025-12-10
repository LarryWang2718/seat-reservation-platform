# Resilient Ticketing Platform Development Schedule

Use this file as the working checklist for the project. At the end of each day, mark completed items with `[x]` and leave unfinished items as `[ ]`.

## Current focus

- [ ] Keep the app booting cleanly with `mvn test`
- [ ] Move DB credentials out of `application.properties`
- [ ] Keep schema changes in Flyway migrations only

## Phase 1: Foundation and event model

- [ ] Define package structure for `event`, `seat`, `hold`, `order`, `ticket`, `payment`, `outbox`
- [ ] Implement `EventStatus`
- [ ] Implement `Event` JPA entity
- [ ] Implement `EventRepository`
- [ ] Create DTOs for event create/list APIs
- [ ] Implement `POST /events`
- [ ] Implement `GET /events`
- [ ] Add tests for event creation and listing

## Phase 2: Seat inventory model

- [ ] Design assigned-seat schema for one event
- [ ] Create Flyway migration for `seat`
- [ ] Implement `Seat` entity
- [ ] Implement `SeatRepository`
- [ ] Enforce unique seat identity within an event
- [ ] Add seat creation/bootstrap path for an event
- [ ] Add tests for seat persistence and uniqueness rules

## Phase 3: Hold flow and concurrency boundary

- [ ] Design `HoldStatus` values
- [ ] Create Flyway migration for `hold`
- [ ] Implement `Hold` entity
- [ ] Implement `HoldRepository`
- [ ] Add DB constraint/index for one active hold per seat
- [ ] Add service method to start checkout hold
- [ ] Enforce max 4 seats per checkout
- [ ] Enforce one active checkout per user per event
- [ ] Lock seat rows transactionally during hold creation
- [ ] Add tests for successful hold creation
- [ ] Add concurrent tests proving no duplicate active hold on one seat

## Phase 4: Order, ticket, and checkout completion

- [ ] Design `OrderStatus`, `TicketStatus`, `PaymentStatus`
- [ ] Create Flyway migration for `order`
- [ ] Create Flyway migration for `ticket`
- [ ] Create Flyway migration for `payment`
- [ ] Implement `Order` entity and repository
- [ ] Implement `Ticket` entity and repository
- [ ] Implement `Payment` entity and repository
- [ ] Add DB rule preventing double-sell of a seat
- [ ] Implement checkout confirmation transaction
- [ ] Convert valid holds into sold tickets atomically
- [ ] Add tests for successful checkout
- [ ] Add concurrent tests proving a seat cannot be sold twice

## Phase 5: Transactional outbox / write-ahead log

- [ ] Design `outbox_event` schema
- [ ] Create Flyway migration for `outbox_event`
- [ ] Implement `OutboxEvent` entity and repository
- [ ] Emit outbox records in the same transaction as hold creation
- [ ] Emit outbox records in the same transaction as checkout completion
- [ ] Define event payloads for hold created, hold expired, order completed, ticket issued
- [ ] Add tests proving business write and outbox write commit together

## Phase 6: Projection pipeline

- [ ] Design projection checkpoint strategy
- [ ] Create projection table for seat availability
- [ ] Create projection table for user tickets
- [ ] Implement ordered projection consumer
- [ ] Make projection consumer idempotent
- [ ] Store last processed event/checkpoint
- [ ] Add replay capability from outbox/event log
- [ ] Add tests for replay rebuilding seat availability
- [ ] Add tests for replay rebuilding user ticket view

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

- [ ] Define GraphQL schema for events
- [ ] Define GraphQL schema for seat map
- [ ] Define GraphQL mutations for hold and checkout
- [ ] Resolve reads from projection tables
- [ ] Route writes through transactional services
- [ ] Add GraphQL integration tests

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

## Today

- [ ] Implement `EventStatus`
- [ ] Implement `Event`
- [ ] Implement `EventRepository`
