package com.eventdriven.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID customerId,
    @NotEmpty List<@Valid OrderItemRequest> items,
    @NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount,
    @NotBlank String currency) {
}
