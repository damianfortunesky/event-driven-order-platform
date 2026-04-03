package com.eventdriven.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventdriven.notification.infrastructure.kafka.model.FinalEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationMessageFactoryTest {

  private final NotificationMessageFactory factory = new NotificationMessageFactory();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldBuildPaymentRejectedMessage() {
    FinalEventEnvelope event = new FinalEventEnvelope(
        UUID.randomUUID(),
        "PaymentRejected",
        "corr-1",
        objectMapper.createObjectNode()
            .put("orderId", "ord-101")
            .put("customerId", "cus-42")
            .put("reasonCode", "INSUFFICIENT_FUNDS")
    );

    NotificationDraft draft = factory.create(event);

    assertThat(draft.orderId()).isEqualTo("ord-101");
    assertThat(draft.subject()).isEqualTo("Pago rechazado");
    assertThat(draft.body()).contains("INSUFFICIENT_FUNDS");
    assertThat(draft.recipientEmail()).isEqualTo("cus-42@example.invalid");
  }
}
