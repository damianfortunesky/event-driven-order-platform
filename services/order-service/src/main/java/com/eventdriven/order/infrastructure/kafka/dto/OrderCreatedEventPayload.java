package com.eventdriven.order.infrastructure.kafka.dto;

import com.eventdriven.order.domain.model.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEventPayload(
    UUID orderId,
    UUID customerId,
    List<OrderItem> items,
    BigDecimal totalAmount,
    String currency,
    String status) {
}
