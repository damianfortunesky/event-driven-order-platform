package com.eventdriven.order.api.dto;

import com.eventdriven.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID customerId,
    List<OrderItemResponse> items,
    BigDecimal totalAmount,
    String currency,
    OrderStatus status,
    Instant createdAt,
    Instant updatedAt) {
}
