package com.project.seat_reserve.order;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CancellationCleanupScheduler {
    private final CancellationCleanupService cancellationCleanupService;

    @Value("${app.cancellation-cleanup.batch-size:100}")
    private int batchSize;

    @Value("${app.cancellation-cleanup.retention-hours:0}")
    private int retentionHours;

    @Scheduled(
        initialDelayString = "${app.cancellation-cleanup.initial-delay-ms:60000}",
        fixedDelayString = "${app.cancellation-cleanup.fixed-delay-ms:60000}"
    )
    public void cleanupCancelledOrders() {
        cancellationCleanupService.cleanupCancelledOrders(
            batchSize,
            LocalDateTime.now().minusHours(retentionHours)
        );
    }
}
