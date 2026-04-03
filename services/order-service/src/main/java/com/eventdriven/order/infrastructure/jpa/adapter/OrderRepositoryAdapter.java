package com.eventdriven.order.infrastructure.jpa.adapter;

import com.eventdriven.order.application.port.out.OrderRepositoryPort;
import com.eventdriven.order.domain.model.Order;
import com.eventdriven.order.domain.model.OrderItem;
import com.eventdriven.order.infrastructure.jpa.entity.OrderItemJpaEntity;
import com.eventdriven.order.infrastructure.jpa.entity.OrderJpaEntity;
import com.eventdriven.order.infrastructure.jpa.repository.OrderJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {
  private final OrderJpaRepository repository;

  @Override
  public Order save(Order order) {
    return toDomain(repository.save(toEntity(order)));
  }

  @Override
  public Optional<Order> findById(UUID id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public List<Order> findAll() {
    return repository.findAll().stream().map(this::toDomain).toList();
  }

  private OrderJpaEntity toEntity(Order order) {
    OrderJpaEntity entity = new OrderJpaEntity();
    entity.setId(order.id());
    entity.setCustomerId(order.customerId());
    entity.setTotalAmount(order.totalAmount());
    entity.setCurrency(order.currency());
    entity.setStatus(order.status());
    entity.setCreatedAt(order.createdAt());
    entity.setUpdatedAt(order.updatedAt());

    int index = 0;
    for (OrderItem item : order.items()) {
      OrderItemJpaEntity itemEntity = new OrderItemJpaEntity();
      itemEntity.setOrder(entity);
      itemEntity.setLineNumber(index++);
      itemEntity.setProductId(item.productId());
      itemEntity.setQuantity(item.quantity());
      entity.getItems().add(itemEntity);
    }
    return entity;
  }

  private Order toDomain(OrderJpaEntity entity) {
    return new Order(
        entity.getId(),
        entity.getCustomerId(),
        entity.getItems().stream().map(item -> new OrderItem(item.getProductId(), item.getQuantity())).toList(),
        entity.getTotalAmount(),
        entity.getCurrency(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
