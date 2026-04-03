package com.eventdriven.inventory.infrastructure.kafka.model;

import java.util.List;
import java.util.UUID;

public record InventoryResultPayload(
    UUID reservationId,
    UUID paymentEventId,
    UUID orderId,
    String status,
    String reason,
    List<ReservedItemPayload> items
) {
}
