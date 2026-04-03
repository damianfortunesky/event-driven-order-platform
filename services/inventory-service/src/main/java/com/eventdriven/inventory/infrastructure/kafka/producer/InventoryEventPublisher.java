package com.eventdriven.inventory.infrastructure.kafka.producer;

import com.eventdriven.inventory.application.ReservationResult;
import com.eventdriven.inventory.infrastructure.kafka.model.EventEnvelope;
import com.eventdriven.inventory.infrastructure.kafka.model.InventoryResultPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.kafka.topics.inventory-reserved}")
  private String inventoryReservedTopic;

  @Value("${app.kafka.topics.inventory-failed}")
  private String inventoryFailedTopic;

  public InventoryEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
  }

  public void publish(ReservationResult result) {
    String eventType = "RESERVED".equals(result.status()) ? "InventoryReserved" : "InventoryFailed";
    String topic = "RESERVED".equals(result.status()) ? inventoryReservedTopic : inventoryFailedTopic;

    UUID eventId = UUID.nameUUIDFromBytes((result.paymentEventId() + ":" + eventType).getBytes(StandardCharsets.UTF_8));
    EventEnvelope outEvent = new EventEnvelope(
        eventId,
        eventType,
        OffsetDateTime.now(),
        result.correlationId(),
        new InventoryResultPayload(
            result.reservationId(),
            result.paymentEventId(),
            result.orderId(),
            result.status(),
            result.reason(),
            result.items()
        )
    );

    try {
      String serialized = objectMapper.writeValueAsString(outEvent);
      kafkaTemplate.send(topic, result.orderId().toString(), serialized);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Could not serialize inventory event", ex);
    }
  }
}
