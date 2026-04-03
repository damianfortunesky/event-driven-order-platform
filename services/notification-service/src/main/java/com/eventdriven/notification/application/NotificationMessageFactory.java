package com.eventdriven.notification.application;

import com.eventdriven.notification.infrastructure.kafka.model.FinalEventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageFactory {

  public NotificationDraft create(FinalEventEnvelope event) {
    JsonNode payload = event.payload();
    String orderId = payload.path("orderId").asText("UNKNOWN");
    String customerId = payload.path("customerId").asText("UNKNOWN");
    String recipientEmail = payload.path("customerEmail").asText(buildFallbackEmail(customerId));

    return switch (event.eventType()) {
      case "OrderConfirmed" -> new NotificationDraft(
          orderId,
          customerId,
          recipientEmail,
          "Tu orden fue confirmada",
          "La orden %s fue confirmada correctamente.".formatted(orderId)
      );
      case "OrderCancelled" -> {
        String reason = payload.path("reasonCode").asText("UNSPECIFIED");
        yield new NotificationDraft(
            orderId,
            customerId,
            recipientEmail,
            "Tu orden fue cancelada",
            "La orden %s fue cancelada. Motivo: %s.".formatted(orderId, reason)
        );
      }
      case "PaymentRejected" -> {
        String reason = payload.path("reasonCode").asText("PAYMENT_REJECTED");
        yield new NotificationDraft(
            orderId,
            customerId,
            recipientEmail,
            "Pago rechazado",
            "No se pudo procesar el pago de la orden %s. Motivo: %s.".formatted(orderId, reason)
        );
      }
      case "InventoryFailed" -> {
        String reason = payload.path("reasonCode").asText("OUT_OF_STOCK");
        yield new NotificationDraft(
            orderId,
            customerId,
            recipientEmail,
            "Problema con inventario",
            "No pudimos reservar inventario para la orden %s. Motivo: %s.".formatted(orderId, reason)
        );
      }
      default -> throw new IllegalArgumentException("Unsupported event type: " + event.eventType());
    };
  }

  private String buildFallbackEmail(String customerId) {
    return (customerId + "@example.invalid").toLowerCase(Locale.ROOT);
  }
}
