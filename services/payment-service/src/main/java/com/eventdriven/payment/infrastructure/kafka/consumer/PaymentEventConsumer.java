package com.eventdriven.payment.infrastructure.kafka.consumer;

import com.eventdriven.payment.application.PaymentProcessingService;
import com.eventdriven.payment.infrastructure.kafka.model.EventEnvelope;
import com.eventdriven.payment.infrastructure.kafka.model.PaymentRequestedPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

  private final ObjectMapper objectMapper;
  private final PaymentProcessingService paymentProcessingService;

  @KafkaListener(
      topics = {"${app.kafka.topics.order-created}", "${app.kafka.topics.payment-requested}"},
      groupId = "${spring.kafka.consumer.group-id}")
  public void onEvent(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);
      String correlationId = root.path("correlationId").asText();
      MDC.put("correlationId", correlationId);

      JsonNode payloadNode = root.path("payload");
      EventEnvelope event = new EventEnvelope(
          UUID.fromString(root.path("eventId").asText()),
          root.path("eventType").asText(),
          OffsetDateTime.parse(root.path("occurredAt").asText()),
          correlationId,
          new PaymentRequestedPayload(
              UUID.fromString(payloadNode.path("orderId").asText()),
              payloadNode.path("totalAmount").decimalValue(),
              payloadNode.path("currency").asText("USD")
          )
      );

      paymentProcessingService.process(event);
    } catch (Exception ex) {
      log.error("payment.event.consume.error message={} error={}", message, ex.getMessage(), ex);
      throw new IllegalStateException("Failed to process incoming payment event", ex);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
