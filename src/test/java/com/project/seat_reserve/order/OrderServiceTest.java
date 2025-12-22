package com.project.seat_reserve.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import com.project.seat_reserve.common.exception.InvalidSessionIdException;
import com.project.seat_reserve.event.Event;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.event.EventStatus;
import com.project.seat_reserve.order.dto.CreateOrderRequest;
import com.project.seat_reserve.order.dto.OrderResponse;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventRepository eventRepository;

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
}
