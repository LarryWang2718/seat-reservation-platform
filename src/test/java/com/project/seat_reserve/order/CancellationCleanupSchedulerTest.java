package com.project.seat_reserve.order;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CancellationCleanupSchedulerTest {
    @Mock
    private CancellationCleanupService cancellationCleanupService;

    @InjectMocks
    private CancellationCleanupScheduler cancellationCleanupScheduler;

    @Captor
    private ArgumentCaptor<LocalDateTime> cutoffCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cancellationCleanupScheduler, "batchSize", 50);
        ReflectionTestUtils.setField(cancellationCleanupScheduler, "retentionHours", 0);
    }

    @Test
    void cleanupCancelledOrdersUsesConfiguredBatchSizeAndRetentionWindow() {
        LocalDateTime beforeInvocation = LocalDateTime.now().minusSeconds(1);

        cancellationCleanupScheduler.cleanupCancelledOrders();

        verify(cancellationCleanupService, times(1)).cleanupCancelledOrders(org.mockito.ArgumentMatchers.eq(50), cutoffCaptor.capture());
        LocalDateTime afterInvocation = LocalDateTime.now().plusSeconds(1);
        assertFalse(cutoffCaptor.getValue().isBefore(beforeInvocation));
        assertFalse(cutoffCaptor.getValue().isAfter(afterInvocation));
    }
}
