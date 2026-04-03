package com.eventdriven.inventory.infrastructure.kafka.model;

import java.util.UUID;

public record OrderItemPayload(
    UUID productId,
    long quantity
) {
}
