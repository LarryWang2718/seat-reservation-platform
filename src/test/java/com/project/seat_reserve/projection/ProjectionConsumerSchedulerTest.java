package com.project.seat_reserve.projection;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectionConsumerSchedulerTest {
    @Mock
    private ProjectionConsumerService projectionConsumerService;

    private ProjectionConsumerScheduler projectionConsumerScheduler;

    @BeforeEach
    void setUp() {
        projectionConsumerScheduler = new ProjectionConsumerScheduler(projectionConsumerService);
        ReflectionTestUtils.setField(projectionConsumerScheduler, "consumerName", "seat-and-ticket-projection");
        ReflectionTestUtils.setField(projectionConsumerScheduler, "batchSize", 2);
        ReflectionTestUtils.setField(projectionConsumerScheduler, "maxBatchesPerRun", 3);
    }

    @Test
    void processOutboxEventsStopsWhenBatchIsNotFull() {
        when(projectionConsumerService.processNextBatch("seat-and-ticket-projection", 2))
            .thenReturn(2)
            .thenReturn(1);

        projectionConsumerScheduler.processOutboxEvents();

        verify(projectionConsumerService, times(2)).processNextBatch("seat-and-ticket-projection", 2);
    }

    @Test
    void processOutboxEventsStopsAtConfiguredBatchLimitWhenBacklogRemains() {
        when(projectionConsumerService.processNextBatch("seat-and-ticket-projection", 2))
            .thenReturn(2)
            .thenReturn(2)
            .thenReturn(2);

        projectionConsumerScheduler.processOutboxEvents();

        verify(projectionConsumerService, times(3)).processNextBatch("seat-and-ticket-projection", 2);
    }
}
