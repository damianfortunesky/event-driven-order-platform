package com.eventdriven.notification.infrastructure.kafka.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record FinalEventEnvelope(
    UUID eventId,
    String eventType,
    String correlationId,
    JsonNode payload
) {
}
