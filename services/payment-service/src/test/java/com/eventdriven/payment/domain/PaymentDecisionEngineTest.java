package com.eventdriven.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentDecisionEngineTest {

  @Test
  void shouldRejectWhenAmountIsAboveThreshold() {
    PaymentDecisionEngine engine = new PaymentDecisionEngine(BigDecimal.valueOf(100), 0);

    PaymentDecision decision = engine.evaluate(UUID.randomUUID(), BigDecimal.valueOf(150));

    assertThat(decision.status()).isEqualTo(PaymentStatus.REJECTED);
    assertThat(decision.reason()).isEqualTo("amount_above_threshold");
  }

  @Test
  void shouldApproveWhenAmountWithinThresholdAndNoRandomReject() {
    PaymentDecisionEngine engine = new PaymentDecisionEngine(BigDecimal.valueOf(1000), 0);

    PaymentDecision decision = engine.evaluate(UUID.fromString("00000000-0000-0000-0000-000000000001"), BigDecimal.valueOf(99));

    assertThat(decision.status()).isEqualTo(PaymentStatus.APPROVED);
    assertThat(decision.reason()).isNull();
  }
}
