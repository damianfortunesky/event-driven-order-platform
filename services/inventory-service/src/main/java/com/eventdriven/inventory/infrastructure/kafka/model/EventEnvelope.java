package com.eventdriven.inventory.infrastructure.kafka.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventEnvelope(
    UUID eventId,
    String eventType,
    OffsetDateTime occurredAt,
    String correlationId,
    Object payload
) {
}
