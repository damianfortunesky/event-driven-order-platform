package com.eventdriven.order.domain.model;

import java.util.UUID;

public record OrderItem(UUID productId, int quantity) {
}
