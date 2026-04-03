package com.eventdriven.order.api.dto;

import java.util.UUID;

public record OrderItemResponse(UUID productId, int quantity) {
}
