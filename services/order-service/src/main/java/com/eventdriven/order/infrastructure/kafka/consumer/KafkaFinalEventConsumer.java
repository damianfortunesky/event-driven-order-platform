package com.eventdriven.order.infrastructure.kafka.consumer;

import com.eventdriven.order.application.port.in.ProcessFinalEventCommand;
import com.eventdriven.order.application.usecase.OrderUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
  private final MeterRegistry meterRegistry;

  @KafkaListener(
      topics = {"${app.kafka.topics.payment-processed}", "${app.kafka.topics.inventory-processed}"},
      groupId = "${spring.kafka.consumer.group-id}")
  public void onEvent(String message) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String eventType = "unknown";
    try {
      JsonNode root = objectMapper.readTree(message);
      String correlationId = root.path("correlationId").asText();
      eventType = root.path("eventType").asText("unknown");
      MDC.put("correlationId", correlationId);

      JsonNode payload = root.path("payload");
      ProcessFinalEventCommand command = new ProcessFinalEventCommand(
          UUID.fromString(root.path("eventId").asText()),
          UUID.fromString(payload.path("orderId").asText()),
          eventType,
          payload.path("status").asText(),
          correlationId
      );

      meterRegistry.counter("eda.events.consumed.total", "event_type", eventType).increment();
      orderUseCase.processFinalEvent(command);
      sample.stop(Timer.builder("eda.event.processing.latency")
          .tag("event_type", eventType)
          .tag("result", "success")
          .register(meterRegistry));
    } catch (Exception ex) {
      meterRegistry.counter("eda.events.consume.errors.total", "event_type", eventType).increment();
      sample.stop(Timer.builder("eda.event.processing.latency")
          .tag("event_type", eventType)
          .tag("result", "error")
          .register(meterRegistry));
      log.error("kafka.event.consume.error message={} error={}", message, ex.getMessage(), ex);
      throw new IllegalStateException("Failed to process event", ex);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
