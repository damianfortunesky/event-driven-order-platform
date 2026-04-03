package com.eventdriven.order.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventdriven.order.application.port.in.CreateOrderCommand;
import com.eventdriven.order.application.usecase.OrderUseCase;
import com.eventdriven.order.domain.model.Order;
import com.eventdriven.order.domain.model.OrderItem;
import com.eventdriven.order.domain.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"payments.payment-processed.v1", "inventory.inventory-processed.v1"})
@ActiveProfiles("test")
class KafkaFinalEventConsumerIntegrationTest {

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private OrderUseCase orderUseCase;

  @MockBean
  private com.eventdriven.order.application.port.out.OrderEventPublisherPort orderEventPublisherPort;

  @Test
  void shouldConsumePaymentProcessedAndUpdateOrderStatus() throws Exception {
    Order order = orderUseCase.createOrder(new CreateOrderCommand(
        UUID.randomUUID(), List.of(new OrderItem(UUID.randomUUID(), 1)), BigDecimal.valueOf(10), "USD", "corr-10"));

    String event = objectMapper.writeValueAsString(Map.of(
        "eventId", UUID.randomUUID().toString(),
        "eventType", "PaymentProcessed",
        "occurredAt", Instant.now().toString(),
        "correlationId", "corr-10",
        "payload", Map.of("orderId", order.id().toString(), "status", "APPROVED")
    ));

    kafkaTemplate.send("payments.payment-processed.v1", order.id().toString(), event);

    Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      Order updated = orderUseCase.getOrder(order.id());
      assertThat(updated.status()).isEqualTo(OrderStatus.PAYMENT_APPROVED);
    });
  }
}
