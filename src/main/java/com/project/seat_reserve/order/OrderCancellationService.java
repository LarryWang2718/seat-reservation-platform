package com.project.seat_reserve.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {
    private final OrderRepository orderRepository;
    private final HoldRepository holdRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        List<Hold> holds = holdRepository.findByOrderId(orderId);

        List<Hold> cancellableHolds = holds.stream()
            .filter(h -> h.getStatus() != HoldStatus.CONFIRMED)
            .toList();
        cancellableHolds.forEach(Hold::markCancelled);
        order.markCancelled();

        holdRepository.saveAll(cancellableHolds);
        orderRepository.save(order);
    }
}
