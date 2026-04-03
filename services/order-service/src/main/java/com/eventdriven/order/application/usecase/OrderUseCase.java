package com.eventdriven.order.application.usecase;

import com.eventdriven.order.application.port.in.CreateOrderCommand;
import com.eventdriven.order.application.port.in.ProcessFinalEventCommand;
import com.eventdriven.order.domain.model.Order;
import java.util.List;
import java.util.UUID;

public interface OrderUseCase {
  Order createOrder(CreateOrderCommand command);

  Order getOrder(UUID id);

  List<Order> listOrders();

  void processFinalEvent(ProcessFinalEventCommand command);
}
