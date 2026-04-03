package com.eventdriven.order.application.port.in;

import java.util.UUID;

public record ProcessFinalEventCommand(
    UUID eventId,
    UUID orderId,
    String eventType,
    String outcome,
    String correlationId) {
}
