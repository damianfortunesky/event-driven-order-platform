package com.eventdriven.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OrderItemRequest(
    @NotNull UUID productId,
    @Min(1) int quantity) {
}
