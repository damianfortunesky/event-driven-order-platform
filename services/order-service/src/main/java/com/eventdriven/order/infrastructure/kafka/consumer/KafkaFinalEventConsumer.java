package com.eventdriven.order.infrastructure.kafka.consumer;

import com.eventdriven.order.application.port.in.ProcessFinalEventCommand;
import com.eventdriven.order.application.usecase.OrderUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaFinalEventConsumer {

  private final ObjectMapper objectMapper;
  private final OrderUseCase orderUseCase;

  @KafkaListener(
      topics = {"${app.kafka.topics.payment-processed}", "${app.kafka.topics.inventory-processed}"},
      groupId = "${spring.kafka.consumer.group-id}")
  public void onEvent(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);
      String correlationId = root.path("correlationId").asText();
      MDC.put("correlationId", correlationId);

      JsonNode payload = root.path("payload");
      ProcessFinalEventCommand command = new ProcessFinalEventCommand(
          UUID.fromString(root.path("eventId").asText()),
          UUID.fromString(payload.path("orderId").asText()),
          root.path("eventType").asText(),
          payload.path("status").asText(),
          correlationId
      );

      orderUseCase.processFinalEvent(command);
    } catch (Exception ex) {
      log.error("kafka.event.consume.error message={} error={}", message, ex.getMessage(), ex);
      throw new IllegalStateException("Failed to process event", ex);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
