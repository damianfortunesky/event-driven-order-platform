package com.eventdriven.inventory.infrastructure.kafka.consumer;

import com.eventdriven.inventory.application.InventoryReservationService;
import com.eventdriven.inventory.application.ReservationResult;
import com.eventdriven.inventory.infrastructure.kafka.model.EventEnvelope;
import com.eventdriven.inventory.infrastructure.kafka.model.OrderItemPayload;
import com.eventdriven.inventory.infrastructure.kafka.model.PaymentApprovedPayload;
import com.eventdriven.inventory.infrastructure.kafka.producer.InventoryEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentApprovedConsumer {

  private final ObjectMapper objectMapper;
  private final InventoryReservationService inventoryReservationService;
  private final InventoryEventPublisher inventoryEventPublisher;

  public PaymentApprovedConsumer(
      ObjectMapper objectMapper,
      InventoryReservationService inventoryReservationService,
      InventoryEventPublisher inventoryEventPublisher
  ) {
    this.objectMapper = objectMapper;
    this.inventoryReservationService = inventoryReservationService;
    this.inventoryEventPublisher = inventoryEventPublisher;
  }

  @KafkaListener(topics = "${app.kafka.topics.payment-approved}", groupId = "${spring.kafka.consumer.group-id}")
  public void onPaymentApproved(String message) {
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

      ReservationResult result = inventoryReservationService.reserveFromPaymentApproved(event, payload);
      inventoryEventPublisher.publish(result);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to process payment-approved event", ex);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
