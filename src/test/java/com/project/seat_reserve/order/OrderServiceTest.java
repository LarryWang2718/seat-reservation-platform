package com.project.seat_reserve.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.seat_reserve.common.exception.ActiveOrderAlreadyExistsException;
import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.common.exception.EventNotOpenForOrderingException;
import com.project.seat_reserve.common.exception.EventSaleWindowClosedException;
import com.project.seat_reserve.common.exception.InvalidHoldState;
import com.project.seat_reserve.common.exception.InvalidSessionIdException;
import com.project.seat_reserve.common.exception.NoActiveHoldsForOrderException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.order.dto.CreateOrderRequest;
import com.project.seat_reserve.order.dto.OrderResponse;
import com.project.seat_reserve.seat.Seat;
import com.project.seat_reserve.ticket.TicketRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OrderCancellationService orderCancellationService;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    void createOrderBuildsPendingOrderFromRequest() {
        Event event = buildEvent(1L, EventStatus.ON_SALE, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(3));
        CreateOrderRequest request = new CreateOrderRequest("session-123", 1L);

        Order savedOrder = new Order();
        savedOrder.setId(100L);
        savedOrder.setEvent(event);
        savedOrder.setSessionId("session-123");
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setCreatedAt(LocalDateTime.now());

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(orderRepository.existsBySessionIdAndEventIdAndStatus("session-123", 1L, OrderStatus.PENDING)).thenReturn(false);
        when(orderRepository.save(orderCaptor.capture())).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(request);
        Order capturedOrder = orderCaptor.getValue();

        assertEquals(event, capturedOrder.getEvent());
        assertEquals("session-123", capturedOrder.getSessionId());
        assertEquals(OrderStatus.PENDING, capturedOrder.getStatus());

        assertEquals(100L, response.getId());
        assertEquals(1L, response.getEventId());
        assertEquals("session-123", response.getSessionId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
    }

    @Test
    void createOrderRejectsMissingEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> orderService.createOrder(new CreateOrderRequest("session-123", 99L)));
    }

    @Test
    void createOrderRejectsBlankSessionId() {
        Event event = buildEvent(1L, EventStatus.ON_SALE, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(3));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(InvalidSessionIdException.class, () -> orderService.createOrder(new CreateOrderRequest("   ", 1L)));
    }

    @Test
    void createOrderRejectsDuplicatePendingOrderForSameSessionAndEvent() {
        Event event = buildEvent(1L, EventStatus.ON_SALE, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(3));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(orderRepository.existsBySessionIdAndEventIdAndStatus("session-123", 1L, OrderStatus.PENDING)).thenReturn(true);

        assertThrows(ActiveOrderAlreadyExistsException.class, () -> orderService.createOrder(new CreateOrderRequest("session-123", 1L)));
    }

    @Test
    void createOrderRejectsEventBeforeSaleWindowOpens() {
        Event event = buildEvent(1L, EventStatus.PUBLISHED, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(orderRepository.existsBySessionIdAndEventIdAndStatus("session-123", 1L, OrderStatus.PENDING)).thenReturn(false);

        assertThrows(EventNotOpenForOrderingException.class, () -> orderService.createOrder(new CreateOrderRequest("session-123", 1L)));
    }

    @Test
    void createOrderRejectsClosedSaleWindow() {
        Event event = buildEvent(1L, EventStatus.ON_SALE, LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(orderRepository.existsBySessionIdAndEventIdAndStatus("session-123", 1L, OrderStatus.PENDING)).thenReturn(false);

        assertThrows(EventSaleWindowClosedException.class, () -> orderService.createOrder(new CreateOrderRequest("session-123", 1L)));
    }

    @Test
    void createOrderRejectsUnavailableEventStatus() {
        Event event = buildEvent(1L, EventStatus.CANCELLED, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(3));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(orderRepository.existsBySessionIdAndEventIdAndStatus("session-123", 1L, OrderStatus.PENDING)).thenReturn(false);

        assertThrows(EventNotOpenForOrderingException.class, () -> orderService.createOrder(new CreateOrderRequest("session-123", 1L)));
    }

    @Test
    void confirmOrderCompletesOrderAndMarksHoldsConfirmed() {
        Event event = buildEvent(1L, EventStatus.ON_SALE, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(3));
        Order order = buildOrder(10L, event, OrderStatus.PENDING);
        Hold firstHold = buildHold(100L, order, 200L, HoldStatus.HELD, LocalDateTime.now().plusMinutes(5));
        Hold secondHold = buildHold(101L, order, 201L, HoldStatus.HELD, LocalDateTime.now().plusMinutes(5));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(holdRepository.findByOrderId(10L)).thenReturn(List.of(firstHold, secondHold));
        when(ticketRepository.existsBySeatId(200L)).thenReturn(false);
        when(ticketRepository.existsBySeatId(201L)).thenReturn(false);

        OrderResponse response = orderService.confirmOrder(10L);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(HoldStatus.CONFIRMED, firstHold.getStatus());
        assertEquals(HoldStatus.CONFIRMED, secondHold.getStatus());
        assertEquals(OrderStatus.COMPLETED, response.getStatus());
        verify(ticketRepository).saveAll(anyList());
        verify(orderCancellationService, never()).cancelOrder(10L);
    }

    @Test
    void confirmOrderCancelsOrderWhenHoldValidationFails() {
        Event event = buildEvent(1L, EventStatus.ON_SALE, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(3));
        Order order = buildOrder(10L, event, OrderStatus.PENDING);
        Hold expiredHold = buildHold(100L, order, 200L, HoldStatus.HELD, LocalDateTime.now().minusMinutes(1));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(holdRepository.findByOrderId(10L)).thenReturn(List.of(expiredHold));

        assertThrows(InvalidHoldState.class, () -> orderService.confirmOrder(10L));

        verify(orderCancellationService).cancelOrder(10L);
        verify(ticketRepository, never()).saveAll(anyList());
    }

    @Test
    void confirmOrderRejectsWhenAllHoldsAreNoLongerActive() {
        Event event = buildEvent(1L, EventStatus.ON_SALE, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(3));
        Order order = buildOrder(10L, event, OrderStatus.PENDING);
        Hold cancelledHold = buildHold(100L, order, 200L, HoldStatus.CANCELLED, LocalDateTime.now().plusMinutes(5));
        Hold expiredHold = buildHold(101L, order, 201L, HoldStatus.EXPIRED, LocalDateTime.now().minusMinutes(1));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(holdRepository.findByOrderId(10L)).thenReturn(List.of(cancelledHold, expiredHold));

        assertThrows(NoActiveHoldsForOrderException.class, () -> orderService.confirmOrder(10L));

        verify(orderCancellationService).cancelOrder(10L);
        verify(ticketRepository, never()).saveAll(anyList());
    }

    @Test
    void confirmOrderCancelsOrderWhenTicketSaveFails() {
        Event event = buildEvent(1L, EventStatus.ON_SALE, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(3));
        Order order = buildOrder(10L, event, OrderStatus.PENDING);
        Hold hold = buildHold(100L, order, 200L, HoldStatus.HELD, LocalDateTime.now().plusMinutes(5));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(holdRepository.findByOrderId(10L)).thenReturn(List.of(hold));
        when(ticketRepository.existsBySeatId(200L)).thenReturn(false);
        when(ticketRepository.saveAll(anyList())).thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class, () -> orderService.confirmOrder(10L));

        verify(orderCancellationService).cancelOrder(10L);
    }

    private Event buildEvent(Long eventId, EventStatus status, LocalDateTime saleStartTime, LocalDateTime saleEndTime) {
        Event event = new Event();
        event.setId(eventId);
        event.setName("Concert");
        event.setStartTime(saleEndTime.plusHours(2));
        event.setEndTime(saleEndTime.plusHours(4));
        event.setSaleStartTime(saleStartTime);
        event.setSaleEndTime(saleEndTime);
        event.setLocation("Arena");
        event.setStatus(status);
        return event;
    }

    private Order buildOrder(Long orderId, Event event, OrderStatus status) {
        Order order = new Order();
        order.setId(orderId);
        order.setEvent(event);
        order.setSessionId("session-123");
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private Hold buildHold(Long holdId, Order order, Long seatId, HoldStatus status, LocalDateTime expiresAt) {
        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setEvent(order.getEvent());
        seat.setSection("A");
        seat.setRowLabel("1");
        seat.setSeatNumber(String.valueOf(seatId));

        Hold hold = new Hold();
        hold.setId(holdId);
        hold.setOrder(order);
        hold.setSeat(seat);
        hold.setCreatedAt(LocalDateTime.now());
        hold.setExpiresAt(expiresAt);
        hold.setStatus(status);
        return hold;
    }
}
