package com.eventdriven.notification.domain;

import java.util.Arrays;

public enum NotificationEventType {
  ORDER_CONFIRMED("OrderConfirmed"),
  ORDER_CANCELLED("OrderCancelled"),
  PAYMENT_REJECTED("PaymentRejected"),
  INVENTORY_FAILED("InventoryFailed");

  private final String eventType;

  NotificationEventType(String eventType) {
    this.eventType = eventType;
  }

  public String eventType() {
    return eventType;
  }

  public static boolean isSupported(String value) {
    return Arrays.stream(values()).anyMatch(type -> type.eventType.equalsIgnoreCase(value));
  }
}
