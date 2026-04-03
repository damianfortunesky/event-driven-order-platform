package com.eventdriven.payment.domain;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentDecisionEngine {

  private final BigDecimal approvalThreshold;
  private final int rejectionPercentage;

  public PaymentDecisionEngine(
      @Value("${app.payment.approval-threshold:1000}") BigDecimal approvalThreshold,
      @Value("${app.payment.rejection-percentage:5}") int rejectionPercentage) {
    this.approvalThreshold = approvalThreshold;
    this.rejectionPercentage = rejectionPercentage;
  }

  public PaymentDecision evaluate(UUID eventId, BigDecimal totalAmount) {
    if (totalAmount == null || totalAmount.signum() <= 0) {
      return PaymentDecision.rejected("invalid_amount");
    }

    if (totalAmount.compareTo(approvalThreshold) >= 0) {
      return PaymentDecision.rejected("amount_above_threshold");
    }

    int bucket = Math.floorMod(eventId.hashCode(), 100);
    if (bucket < rejectionPercentage) {
      return PaymentDecision.rejected("randomized_rejection_for_testing");
    }

    return PaymentDecision.approved();
  }
}
