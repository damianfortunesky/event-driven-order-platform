package com.eventdriven.order.infrastructure.kafka.producer;

import com.eventdriven.order.application.port.out.OrderEventPublisherPort;
import com.eventdriven.order.domain.model.Order;
import com.eventdriven.order.infrastructure.kafka.dto.OrderCreatedEventPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderEventPublisher implements OrderEventPublisherPort {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.kafka.topics.order-created}")
  private String orderCreatedTopic;

  @Override
  public void publishOrderCreated(Order order, String correlationId) {
    OrderCreatedEventPayload payload = new OrderCreatedEventPayload(
        order.id(),
        order.customerId(),
        order.items(),
        order.totalAmount(),
        order.currency(),
        order.status().name());

    Map<String, Object> envelope = Map.of(
        "eventId", UUID.randomUUID(),
        "eventType", "OrderCreated",
        "occurredAt", Instant.now(),
        "correlationId", correlationId,
        "payload", payload
    );

    try {
      String message = objectMapper.writeValueAsString(envelope);
      kafkaTemplate.send(orderCreatedTopic, order.id().toString(), message);
      log.info("kafka.event.published topic={} eventType=OrderCreated orderId={}", orderCreatedTopic, order.id());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Error serializing OrderCreated event", e);
    }
  }
}
