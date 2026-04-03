package com.eventdriven.order.api;

import com.eventdriven.order.api.dto.CreateOrderRequest;
import com.eventdriven.order.api.dto.OrderItemResponse;
import com.eventdriven.order.api.dto.OrderResponse;
import com.eventdriven.order.application.port.in.CreateOrderCommand;
import com.eventdriven.order.domain.model.Order;
import com.eventdriven.order.domain.model.OrderItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

  public CreateOrderCommand toCommand(CreateOrderRequest request, String correlationId) {
    List<OrderItem> items = request.items().stream()
        .map(item -> new OrderItem(item.productId(), item.quantity()))
        .toList();

    return new CreateOrderCommand(request.customerId(), items, request.totalAmount(), request.currency(), correlationId);
  }

  public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.id(),
        order.customerId(),
        order.items().stream().map(i -> new OrderItemResponse(i.productId(), i.quantity())).toList(),
        order.totalAmount(),
        order.currency(),
        order.status(),
        order.createdAt(),
        order.updatedAt());
  }
}
