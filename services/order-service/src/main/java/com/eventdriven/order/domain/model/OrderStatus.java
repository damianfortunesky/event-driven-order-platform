package com.eventdriven.order.domain.model;

public enum OrderStatus {
  PENDING,
  PAYMENT_APPROVED,
  PAYMENT_REJECTED,
  INVENTORY_RESERVED,
  INVENTORY_FAILED,
  CONFIRMED,
  CANCELLED
}
