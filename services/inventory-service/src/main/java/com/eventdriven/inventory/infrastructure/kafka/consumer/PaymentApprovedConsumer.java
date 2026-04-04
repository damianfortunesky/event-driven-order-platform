package com.eventdriven.inventory.infrastructure.kafka.consumer;

import com.eventdriven.inventory.application.InventoryReservationService;
import com.eventdriven.inventory.application.ReservationResult;
import com.eventdriven.inventory.infrastructure.kafka.model.EventEnvelope;
import com.eventdriven.inventory.infrastructure.kafka.model.OrderItemPayload;
import com.eventdriven.inventory.infrastructure.kafka.model.PaymentApprovedPayload;
import com.eventdriven.inventory.infrastructure.kafka.producer.InventoryEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentApprovedConsumer {

  private final ObjectMapper objectMapper;
  private final InventoryReservationService inventoryReservationService;
  private final InventoryEventPublisher inventoryEventPublisher;
  private final MeterRegistry meterRegistry;

  public PaymentApprovedConsumer(
      ObjectMapper objectMapper,
      InventoryReservationService inventoryReservationService,
      InventoryEventPublisher inventoryEventPublisher,
      MeterRegistry meterRegistry
  ) {
    this.objectMapper = objectMapper;
    this.inventoryReservationService = inventoryReservationService;
    this.inventoryEventPublisher = inventoryEventPublisher;
    this.meterRegistry = meterRegistry;
  }

  @KafkaListener(topics = "${app.kafka.topics.payment-approved}", groupId = "${spring.kafka.consumer.group-id}")
  public void onPaymentApproved(String message) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String eventType = "PaymentApproved";
    try {
      JsonNode root = objectMapper.readTree(message);
      String correlationId = root.path("correlationId").asText("no-correlation-id");
      MDC.put("correlationId", correlationId);

      JsonNode payloadNode = root.path("payload");
      List<OrderItemPayload> items = new ArrayList<>();
      if (payloadNode.has("items") && payloadNode.get("items").isArray()) {
        for (JsonNode itemNode : payloadNode.get("items")) {
          items.add(new OrderItemPayload(
              UUID.fromString(itemNode.path("productId").asText()),
              itemNode.path("quantity").asLong()
          ));
        }
      }

      PaymentApprovedPayload payload = new PaymentApprovedPayload(
          payloadNode.hasNonNull("paymentId") ? UUID.fromString(payloadNode.path("paymentId").asText()) : null,
          UUID.fromString(payloadNode.path("orderId").asText()),
          payloadNode.path("status").asText("APPROVED"),
          items
      );

      EventEnvelope event = new EventEnvelope(
          UUID.fromString(root.path("eventId").asText()),
          root.path("eventType").asText(),
          OffsetDateTime.parse(root.path("occurredAt").asText()),
          correlationId,
          payload
      );
      eventType = event.eventType();

      meterRegistry.counter("eda.events.consumed.total", "event_type", event.eventType()).increment();
      ReservationResult result = inventoryReservationService.reserveFromPaymentApproved(event, payload);
      inventoryEventPublisher.publish(result);
      sample.stop(Timer.builder("eda.event.processing.latency")
          .tag("event_type", event.eventType())
          .tag("result", "success")
          .register(meterRegistry));
    } catch (Exception ex) {
      meterRegistry.counter("eda.events.consume.errors.total", "event_type", eventType).increment();
      sample.stop(Timer.builder("eda.event.processing.latency")
          .tag("event_type", eventType)
          .tag("result", "error")
          .register(meterRegistry));
      log.error("inventory.event.consume.error message={} error={}", message, ex.getMessage(), ex);
      throw new IllegalStateException("Failed to process payment-approved event", ex);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
