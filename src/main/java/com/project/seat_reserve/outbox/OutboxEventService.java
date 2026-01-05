package com.project.seat_reserve.outbox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.ticket.Ticket;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEvent publishHoldCreated(Hold hold) {
        HoldCreatedPayload payload = new HoldCreatedPayload(
            hold.getId(),
            hold.getOrder().getId(),
            hold.getOrder().getEvent().getId(),
            hold.getSeat().getId(),
            hold.getOrder().getSessionId(),
            hold.getCreatedAt(),
            hold.getExpiresAt()
        );

        return outboxEventRepository.save(OutboxEvent.create(
            OutboxAggregateType.HOLD,
            hold.getId(),
            OutboxEventType.HOLD_CREATED,
            serialize(payload),
            hold.getCreatedAt()
        ));
    }

    public List<OutboxEvent> publishOrderCompleted(Order order, List<Hold> holds, List<Ticket> tickets, LocalDateTime completedAt) {
        List<OutboxEvent> outboxEvents = new ArrayList<>();

        OrderCompletedPayload orderPayload = new OrderCompletedPayload(
            order.getId(),
            order.getEvent().getId(),
            order.getSessionId(),
            holds.stream().map(Hold::getId).toList(),
            tickets.stream().map(Ticket::getId).toList(),
            completedAt
        );
        outboxEvents.add(OutboxEvent.create(
            OutboxAggregateType.ORDER,
            order.getId(),
            OutboxEventType.ORDER_COMPLETED,
            serialize(orderPayload),
            completedAt
        ));

        for (Ticket ticket : tickets) {
            TicketIssuedPayload ticketPayload = new TicketIssuedPayload(
                ticket.getId(),
                order.getId(),
                order.getEvent().getId(),
                ticket.getSeat().getId(),
                ticket.getSeat().getSection(),
                ticket.getSeat().getRowLabel(),
                ticket.getSeat().getSeatNumber(),
                completedAt
            );
            outboxEvents.add(OutboxEvent.create(
                OutboxAggregateType.TICKET,
                ticket.getId(),
                OutboxEventType.TICKET_ISSUED,
                serialize(ticketPayload),
                completedAt
            ));
        }

        return outboxEventRepository.saveAll(outboxEvents);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox payload", exception);
        }
    }
}
