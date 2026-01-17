package com.project.seat_reserve.hold;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.project.seat_reserve.observability.ReservationMetrics;
import com.project.seat_reserve.outbox.OutboxEventService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class HoldExpirationService {
    private final HoldRepository holdRepository;
    private final OutboxEventService outboxEventService;
    private final ReservationMetrics reservationMetrics;

    @Transactional
    public int expireStaleHolds(int batchSize) {
        LocalDateTime now = LocalDateTime.now();
        List<Hold> expiredHolds = holdRepository.findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            HoldStatus.HELD,
            now,
            PageRequest.of(0, batchSize)
        );

        if (expiredHolds.isEmpty()) {
            return 0;
        }

        for (Hold hold : expiredHolds) {
            hold.markExpired();
            outboxEventService.publishHoldExpired(hold, now);
        }

        holdRepository.saveAll(expiredHolds);
        reservationMetrics.recordExpiredHolds(expiredHolds.size());
        log.info("Expired stale holds: count={}, batchSize={}, expiredAt={}", expiredHolds.size(), batchSize, now);
        return expiredHolds.size();
    }
}
