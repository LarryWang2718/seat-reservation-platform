package com.project.seat_reserve.projection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectionConsumerScheduler {
    private final ProjectionConsumerService projectionConsumerService;

    @Value("${app.projection-consumer.consumer-name:seat-and-ticket-projection}")
    private String consumerName;

    @Value("${app.projection-consumer.batch-size:100}")
    private int batchSize;

    @Value("${app.projection-consumer.max-batches-per-run:10}")
    private int maxBatchesPerRun;

    @Scheduled(
        initialDelayString = "${app.projection-consumer.initial-delay-ms:5000}",
        fixedDelayString = "${app.projection-consumer.fixed-delay-ms:5000}"
    )
    public void processOutboxEvents() {
        for (int batchNumber = 0; batchNumber < maxBatchesPerRun; batchNumber++) {
            int processedCount = projectionConsumerService.processNextBatch(consumerName, batchSize);
            if (processedCount < batchSize) {
                return;
            }
        }
    }
}
