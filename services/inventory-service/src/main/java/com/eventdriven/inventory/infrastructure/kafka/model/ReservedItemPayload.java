package com.eventdriven.inventory.infrastructure.kafka.model;

import java.util.UUID;

public record ReservedItemPayload(
    UUID productId,
    long quantity
) {
}
