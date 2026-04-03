package com.eventdriven.notification.api.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID notificationId,
    UUID sourceEventId,
    String sourceEventType,
    String orderId,
    String customerId,
    String recipientEmail,
    String channel,
    String subject,
    String body,
    String correlationId,
    Instant createdAt
) {
}
