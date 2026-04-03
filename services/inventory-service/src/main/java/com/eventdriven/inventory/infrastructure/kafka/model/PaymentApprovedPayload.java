package com.eventdriven.inventory.infrastructure.kafka.model;

import java.util.List;
import java.util.UUID;

public record PaymentApprovedPayload(
    UUID paymentId,
    UUID orderId,
    String status,
    List<OrderItemPayload> items
) {
}
