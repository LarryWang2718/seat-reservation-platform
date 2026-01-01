package com.project.seat_reserve.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.seat_reserve.event.EventRepository;
import com.project.seat_reserve.hold.Hold;
import com.project.seat_reserve.hold.HoldRepository;
import com.project.seat_reserve.hold.HoldStatus;
import com.project.seat_reserve.order.Order;
import com.project.seat_reserve.order.OrderRepository;
import com.project.seat_reserve.order.OrderStatus;
import com.project.seat_reserve.seat.SeatRepository;
import com.project.seat_reserve.ticket.TicketRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutGraphqlIntegrationTest {
    private static final String CREATE_EVENT = """
        mutation($input: CreateEventInput!) {
          createEvent(input: $input) {
            id
            status
          }
        }
        """;

    private static final String CREATE_SEAT = """
        mutation($input: CreateSeatInput!) {
          createSeat(input: $input) {
            id
            eventId
            section
            rowLabel
            seatNumber
          }
        }
        """;

    private static final String CREATE_ORDER = """
        mutation($input: CreateOrderInput!) {
          createOrder(input: $input) {
            id
            eventId
            sessionId
            status
          }
        }
        """;

    private static final String CREATE_HOLD = """
        mutation($input: CreateHoldInput!) {
          createHold(input: $input) {
            id
            seatId
            orderId
            status
          }
        }
        """;

    private static final String HOLDS_BY_ORDER = """
        query($orderId: ID!) {
          holdsByOrder(orderId: $orderId) {
            id
            seatId
            orderId
            status
          }
        }
        """;

    private static final String CONFIRM_ORDER = """
        mutation($orderId: ID!) {
          confirmOrder(orderId: $orderId) {
            id
            status
          }
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private HoldRepository holdRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAllInBatch();
        holdRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }

    @Test
    void checkoutFlowCreatesHoldAndCompletesOrderThroughGraphql() throws Exception {
        Long eventId = createEvent();
        Long seatId = createSeat(eventId);
        Long orderId = createOrder(eventId, "graphql-session-success");

        JsonNode createHoldResponse = executeGraphQl(CREATE_HOLD, Map.of("input", Map.of("seatId", seatId, "orderId", orderId)));
        assertNoErrors(createHoldResponse);
        assertThat(createHoldResponse.path("data").path("createHold").path("id").asLong()).isPositive();

        JsonNode holdsResponse = executeGraphQl(HOLDS_BY_ORDER, Map.of("orderId", orderId));
        assertNoErrors(holdsResponse);
        JsonNode holds = holdsResponse.path("data").path("holdsByOrder");
        assertThat(holds).hasSize(1);
        assertThat(holds.get(0).path("status").asText()).isEqualTo("HELD");
        assertThat(holds.get(0).path("seatId").asLong()).isEqualTo(seatId);

        JsonNode confirmResponse = executeGraphQl(CONFIRM_ORDER, Map.of("orderId", orderId));
        assertNoErrors(confirmResponse);
        assertThat(confirmResponse.path("data").path("confirmOrder").path("status").asText()).isEqualTo("COMPLETED");

        Order savedOrder = orderRepository.findById(orderId).orElseThrow();
        Hold savedHold = holdRepository.findByOrderId(orderId).get(0);

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(savedHold.getStatus()).isEqualTo(HoldStatus.CONFIRMED);
        assertThat(ticketRepository.existsBySeatId(seatId)).isTrue();
    }

    @Test
    void confirmOrderCancelsCheckoutWhenHoldCannotBeConfirmed() throws Exception {
        Long eventId = createEvent();
        Long seatId = createSeat(eventId);
        Long orderId = createOrder(eventId, "graphql-session-failure");

        JsonNode createHoldResponse = executeGraphQl(CREATE_HOLD, Map.of("input", Map.of("seatId", seatId, "orderId", orderId)));
        assertNoErrors(createHoldResponse);

        Hold hold = holdRepository.findByOrderId(orderId).get(0);
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        holdRepository.save(hold);

        JsonNode confirmResponse = executeGraphQl(CONFIRM_ORDER, Map.of("orderId", orderId));
        JsonNode errors = confirmResponse.path("errors");
        assertThat(errors.isArray()).isTrue();
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0).path("message").asText()).contains("invalid state");
        assertThat(errors.get(0).path("extensions").path("classification").asText()).isEqualTo("BAD_REQUEST");
        assertThat(errors.get(0).path("extensions").path("code").asText()).isEqualTo("InvalidHoldState");

        Order cancelledOrder = orderRepository.findById(orderId).orElseThrow();
        Hold cancelledHold = holdRepository.findByOrderId(orderId).get(0);

        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledHold.getStatus()).isEqualTo(HoldStatus.CANCELLED);
        assertThat(ticketRepository.existsBySeatId(seatId)).isFalse();
    }

    @Test
    void createOrderReturnsNotFoundErrorForMissingEvent() throws Exception {
        JsonNode response = executeGraphQl(CREATE_ORDER, Map.of(
            "input", Map.of(
                "sessionId", "missing-event-session",
                "eventId", 999999L
            )
        ));

        JsonNode errors = response.path("errors");
        assertThat(errors.isArray()).isTrue();
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0).path("message").asText()).isEqualTo("Event not found: 999999");
        assertThat(errors.get(0).path("extensions").path("classification").asText()).isEqualTo("NOT_FOUND");
        assertThat(errors.get(0).path("extensions").path("code").asText()).isEqualTo("EventNotFoundException");
    }

    private Long createEvent() throws Exception {
        LocalDateTime startTime = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime endTime = startTime.plusHours(2);
        LocalDateTime saleStartTime = LocalDateTime.now().minusHours(1).withNano(0);
        LocalDateTime saleEndTime = LocalDateTime.now().plusHours(4).withNano(0);

        JsonNode response = executeGraphQl(CREATE_EVENT, Map.of(
            "input", Map.of(
                "name", "GraphQL Integration Event",
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "saleStartTime", saleStartTime.toString(),
                "saleEndTime", saleEndTime.toString(),
                "location", "Integration Arena"
            )
        ));
        assertNoErrors(response);
        return response.path("data").path("createEvent").path("id").asLong();
    }

    private Long createSeat(Long eventId) throws Exception {
        JsonNode response = executeGraphQl(CREATE_SEAT, Map.of(
            "input", Map.of(
                "eventId", eventId,
                "section", "A",
                "rowLabel", "1",
                "seatNumber", "10"
            )
        ));
        assertNoErrors(response);
        return response.path("data").path("createSeat").path("id").asLong();
    }

    private Long createOrder(Long eventId, String sessionId) throws Exception {
        JsonNode response = executeGraphQl(CREATE_ORDER, Map.of(
            "input", Map.of(
                "sessionId", sessionId,
                "eventId", eventId
            )
        ));
        assertNoErrors(response);
        return response.path("data").path("createOrder").path("id").asLong();
    }

    private JsonNode executeGraphQl(String query, Map<String, Object> variables) throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("query", query, "variables", variables))))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertNoErrors(JsonNode response) {
        assertThat(response.path("errors").isMissingNode() || response.path("errors").isEmpty()).isTrue();
    }
}
