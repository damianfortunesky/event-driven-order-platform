package com.eventdriven.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order {
  private final UUID id;
  private final UUID customerId;
  private final List<OrderItem> items;
  private final BigDecimal totalAmount;
  private final String currency;
  private OrderStatus status;
  private final Instant createdAt;
  private Instant updatedAt;

  public Order(UUID id,
               UUID customerId,
               List<OrderItem> items,
               BigDecimal totalAmount,
               String currency,
               OrderStatus status,
               Instant createdAt,
               Instant updatedAt) {
    this.id = id;
    this.customerId = customerId;
    this.items = List.copyOf(items);
    this.totalAmount = totalAmount;
    this.currency = currency;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Order create(UUID customerId, List<OrderItem> items, BigDecimal totalAmount, String currency) {
    Instant now = Instant.now();
    return new Order(UUID.randomUUID(), customerId, items, totalAmount, currency, OrderStatus.PENDING, now, now);
  }

  public UUID id() {
    return id;
  }

  public UUID customerId() {
    return customerId;
  }

  public List<OrderItem> items() {
    return items;
  }

  public BigDecimal totalAmount() {
    return totalAmount;
  }

  public String currency() {
    return currency;
  }

  public OrderStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public void markPaymentApproved() {
    if (status == OrderStatus.PENDING) {
      status = OrderStatus.PAYMENT_APPROVED;
      updatedAt = Instant.now();
    }
  }

  public void markPaymentRejected() {
    if (status == OrderStatus.PENDING) {
      status = OrderStatus.PAYMENT_REJECTED;
      updatedAt = Instant.now();
    }
  }

  public void markInventoryReserved() {
    if (status == OrderStatus.PAYMENT_APPROVED) {
      status = OrderStatus.INVENTORY_RESERVED;
      updatedAt = Instant.now();
    }
  }

  public void markInventoryFailed() {
    if (status == OrderStatus.PAYMENT_APPROVED) {
      status = OrderStatus.INVENTORY_FAILED;
      updatedAt = Instant.now();
    }
  }

  public void confirm() {
    if (status == OrderStatus.INVENTORY_RESERVED) {
      status = OrderStatus.CONFIRMED;
      updatedAt = Instant.now();
    }
  }

  public void cancel() {
    if (status == OrderStatus.PAYMENT_REJECTED || status == OrderStatus.INVENTORY_FAILED) {
      status = OrderStatus.CANCELLED;
      updatedAt = Instant.now();
    }
  }
}
