package com.eventdriven.order.application.port.out;

import com.eventdriven.order.domain.model.Order;

public interface OrderEventPublisherPort {
  void publishOrderCreated(Order order, String correlationId);
}
