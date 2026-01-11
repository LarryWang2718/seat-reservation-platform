package com.project.seat_reserve.hold;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.project.seat_reserve.outbox.OutboxEventService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldExpirationService {
    private final HoldRepository holdRepository;
    private final OutboxEventService outboxEventService;

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
        return expiredHolds.size();
    }
}
