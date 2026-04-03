package com.eventdriven.order.application.port.in;

import com.eventdriven.order.domain.model.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
    UUID customerId,
    List<OrderItem> items,
    BigDecimal totalAmount,
    String currency,
    String correlationId) {
}
