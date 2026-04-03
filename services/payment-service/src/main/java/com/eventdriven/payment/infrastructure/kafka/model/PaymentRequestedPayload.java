package com.eventdriven.payment.infrastructure.kafka.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestedPayload(
    UUID orderId,
    BigDecimal totalAmount,
    String currency
) {
}
