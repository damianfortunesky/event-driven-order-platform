package com.eventdriven.notification.application;

public record NotificationDraft(
    String orderId,
    String customerId,
    String recipientEmail,
    String subject,
    String body
) {
}
