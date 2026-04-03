package com.eventdriven.payment.domain;

public record PaymentDecision(PaymentStatus status, String reason) {

  public static PaymentDecision approved() {
    return new PaymentDecision(PaymentStatus.APPROVED, null);
  }

  public static PaymentDecision rejected(String reason) {
    return new PaymentDecision(PaymentStatus.REJECTED, reason);
  }
}
