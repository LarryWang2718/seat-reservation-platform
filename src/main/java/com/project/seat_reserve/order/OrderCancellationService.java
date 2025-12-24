package com.project.seat_reserve.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;

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

        holds.forEach(Hold::markCancelled);
        order.markCancelled();

        holdRepository.saveAll(holds);
        orderRepository.save(order);
    }
}
