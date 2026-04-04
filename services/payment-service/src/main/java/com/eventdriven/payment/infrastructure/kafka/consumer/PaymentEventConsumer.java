package com.eventdriven.payment.infrastructure.kafka.consumer;

import com.eventdriven.payment.application.PaymentProcessingService;
import com.eventdriven.payment.infrastructure.kafka.model.EventEnvelope;
import com.eventdriven.payment.infrastructure.kafka.model.PaymentRequestedPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
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

  private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of("OrderCreated", "PaymentRequested");

  private final ObjectMapper objectMapper;
  private final PaymentProcessingService paymentProcessingService;
  private final MeterRegistry meterRegistry;

  @KafkaListener(
      topics = {"${app.kafka.topics.order-created}", "${app.kafka.topics.payment-requested}"},
      groupId = "${spring.kafka.consumer.group-id}")
  public void onEvent(String message) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String eventType = "unknown";
    try {
      EventEnvelope event = parse(message);
      eventType = event.eventType();
      MDC.put("correlationId", event.correlationId());
      meterRegistry.counter("eda.events.consumed.total", "event_type", event.eventType()).increment();
      paymentProcessingService.process(event);
      sample.stop(Timer.builder("eda.event.processing.latency")
          .tag("event_type", event.eventType())
          .tag("result", "success")
          .register(meterRegistry));
    } catch (InvalidPaymentEventException ex) {
      meterRegistry.counter("eda.events.consume.errors.total", "event_type", eventType, "error", "invalid").increment();
      sample.stop(Timer.builder("eda.event.processing.latency")
          .tag("event_type", eventType)
          .tag("result", "error")
          .register(meterRegistry));
      log.warn("payment.event.consume.invalid message={} error={}", message, ex.getMessage());
      throw ex;
    } catch (Exception ex) {
      meterRegistry.counter("eda.events.consume.errors.total", "event_type", eventType, "error", "processing").increment();
      sample.stop(Timer.builder("eda.event.processing.latency")
          .tag("event_type", eventType)
          .tag("result", "error")
          .register(meterRegistry));
      log.error("payment.event.consume.error message={} error={}", message, ex.getMessage(), ex);
      throw new IllegalStateException("Failed to process incoming payment event", ex);
    } finally {
      MDC.remove("correlationId");
    }
  }

  private EventEnvelope parse(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);

      String eventType = requiredText(root, "eventType");
      if (!SUPPORTED_EVENT_TYPES.contains(eventType)) {
        throw new InvalidPaymentEventException("Unsupported eventType: " + eventType);
      }

      JsonNode payloadNode = root.path("payload");
      if (payloadNode.isMissingNode() || payloadNode.isNull()) {
        throw new InvalidPaymentEventException("Missing payload node");
      }

      PaymentRequestedPayload payload = new PaymentRequestedPayload(
          UUID.fromString(requiredText(payloadNode, "orderId")),
          requiredDecimal(payloadNode, "totalAmount"),
          payloadNode.path("currency").asText("USD")
      );

      return new EventEnvelope(
          UUID.fromString(requiredText(root, "eventId")),
          eventType,
          OffsetDateTime.parse(requiredText(root, "occurredAt")),
          root.path("correlationId").asText(UUID.randomUUID().toString()),
          payload
      );
    } catch (InvalidPaymentEventException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InvalidPaymentEventException("Could not parse payment input event", ex);
    }
  }

  private String requiredText(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText("").trim();
    if (value.isEmpty()) {
      throw new InvalidPaymentEventException("Missing required field: " + fieldName);
    }
    return value;
  }

  private BigDecimal requiredDecimal(JsonNode node, String fieldName) {
    if (node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
      throw new InvalidPaymentEventException("Missing required field: " + fieldName);
    }
    return node.path(fieldName).decimalValue();
  }
}
