package com.project.seat_reserve.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.outbox.OutboxEventService;
import com.project.seat_reserve.ticket.TicketRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CancellationCleanupService {
    private final OrderRepository orderRepository;
    private final HoldRepository holdRepository;
    private final TicketRepository ticketRepository;
    private final OutboxEventService outboxEventService;

    @Transactional
    public int cleanupCancelledOrders(int batchSize, LocalDateTime createdBefore) {
        List<Order> candidateOrders = orderRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            OrderStatus.CANCELLED,
            createdBefore,
            PageRequest.of(0, batchSize)
        );

        if (candidateOrders.isEmpty()) {
            return 0;
        }

        List<Long> candidateOrderIds = candidateOrders.stream().map(Order::getId).toList();
        List<Hold> candidateHolds = holdRepository.findByOrderIdIn(candidateOrderIds);
        Set<Long> ordersWithNonCancelledHolds = candidateHolds.stream()
            .filter(hold -> hold.getStatus() != HoldStatus.CANCELLED)
            .map(hold -> hold.getOrder().getId())
            .collect(java.util.stream.Collectors.toSet());
        Set<Long> ordersWithTickets = ticketRepository.findByOrderIdIn(candidateOrderIds).stream()
            .map(ticket -> ticket.getOrder().getId())
            .collect(java.util.stream.Collectors.toSet());

        List<Order> ordersToDelete = candidateOrders.stream()
            .filter(order -> !ordersWithNonCancelledHolds.contains(order.getId()))
            .filter(order -> !ordersWithTickets.contains(order.getId()))
            .toList();
        if (ordersToDelete.isEmpty()) {
            return 0;
        }

        Set<Long> orderIdsToDelete = ordersToDelete.stream()
            .map(Order::getId)
            .collect(java.util.stream.Collectors.toSet());
        List<Hold> cancelledHoldsToDelete = candidateHolds.stream()
            .filter(hold -> hold.getStatus() == HoldStatus.CANCELLED)
            .filter(hold -> orderIdsToDelete.contains(hold.getOrder().getId()))
            .toList();

        LocalDateTime cleanupTime = LocalDateTime.now();
        for (Hold hold : cancelledHoldsToDelete) {
            // Reuse the hold-release event so projection consumers can free seats before rows are purged.
            outboxEventService.publishHoldExpired(hold, cleanupTime);
        }

        holdRepository.deleteAllInBatch(cancelledHoldsToDelete);
        orderRepository.deleteAllInBatch(ordersToDelete);
        return ordersToDelete.size();
    }
}
