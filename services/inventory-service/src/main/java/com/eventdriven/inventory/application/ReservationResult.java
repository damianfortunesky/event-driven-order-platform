package com.eventdriven.inventory.application;

import com.eventdriven.inventory.infrastructure.kafka.model.ReservedItemPayload;
import java.util.List;
import java.util.UUID;

public record ReservationResult(
    UUID reservationId,
    UUID paymentEventId,
    UUID orderId,
    String status,
    String reason,
    String correlationId,
    List<ReservedItemPayload> items
) {
}
