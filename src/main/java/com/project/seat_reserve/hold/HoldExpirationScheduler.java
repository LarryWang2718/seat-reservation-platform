package com.project.seat_reserve.hold;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HoldExpirationScheduler {
    private final HoldExpirationService holdExpirationService;

    @Value("${app.hold-expiration.batch-size:100}")
    private int batchSize;

    @Scheduled(
        initialDelayString = "${app.hold-expiration.initial-delay-ms:30000}",
        fixedDelayString = "${app.hold-expiration.fixed-delay-ms:30000}"
    )
    public void expireStaleHolds() {
        holdExpirationService.expireStaleHolds(batchSize);
    }
}
