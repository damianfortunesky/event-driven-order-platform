package com.eventdriven.order.application.port.out;

import com.eventdriven.order.domain.model.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
  Order save(Order order);

  Optional<Order> findById(UUID id);

  List<Order> findAll();
}
