package com.eventdriven.notification.infrastructure.kafka.consumer;

import com.eventdriven.notification.application.NotificationService;
import com.eventdriven.notification.infrastructure.kafka.model.FinalEventEnvelope;
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
public class FinalEventsConsumer {

  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;
  private final MeterRegistry meterRegistry;

  @KafkaListener(
      topics = {
          "${app.kafka.topics.order-confirmed}",
          "${app.kafka.topics.order-cancelled}",
          "${app.kafka.topics.payment-rejected}",
          "${app.kafka.topics.inventory-failed}"
      },
      groupId = "${spring.kafka.consumer.group-id}"
  )
  public void onEvent(String message) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String eventType = "unknown";
    try {
      JsonNode root = objectMapper.readTree(message);
      String correlationId = root.path("correlationId").asText();
      eventType = root.path("eventType").asText("unknown");
      MDC.put("correlationId", correlationId);

      FinalEventEnvelope event = new FinalEventEnvelope(
          UUID.fromString(root.path("eventId").asText()),
          eventType,
          correlationId,
          root.path("payload")
      );

      meterRegistry.counter("eda.events.consumed.total", "event_type", eventType).increment();
      notificationService.processFinalEvent(event);
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
      log.error("notification.kafka.consume.error message={} error={}", message, ex.getMessage(), ex);
      throw new IllegalStateException("Failed to process final event", ex);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
