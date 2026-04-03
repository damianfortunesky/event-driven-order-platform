package com.eventdriven.order.api;

import com.eventdriven.order.api.dto.CreateOrderRequest;
import com.eventdriven.order.api.dto.OrderResponse;
import com.eventdriven.order.application.usecase.OrderUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderUseCase orderUseCase;
  private final OrderMapper orderMapper;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
    return orderMapper.toResponse(orderUseCase.createOrder(orderMapper.toCommand(request, MDC.get("correlationId"))));
  }

  @GetMapping("/{id}")
  public OrderResponse getById(@PathVariable UUID id) {
    return orderMapper.toResponse(orderUseCase.getOrder(id));
  }

  @GetMapping
  public List<OrderResponse> list() {
    return orderUseCase.listOrders().stream().map(orderMapper::toResponse).toList();
  }
}
