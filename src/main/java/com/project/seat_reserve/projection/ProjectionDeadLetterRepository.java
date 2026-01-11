package com.project.seat_reserve.projection;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectionDeadLetterRepository extends JpaRepository<ProjectionDeadLetter, Long> {
    Optional<ProjectionDeadLetter> findByConsumerNameAndOutboxEventId(String consumerName, Long outboxEventId);
}
