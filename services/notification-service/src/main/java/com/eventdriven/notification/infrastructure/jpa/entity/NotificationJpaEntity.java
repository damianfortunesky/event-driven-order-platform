package com.eventdriven.notification.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

  @Id
  private UUID id;

  @Column(name = "source_event_id", nullable = false)
  private UUID sourceEventId;

  @Column(name = "source_event_type", nullable = false, length = 120)
  private String sourceEventType;

  @Column(name = "order_id", nullable = false, length = 120)
  private String orderId;

  @Column(name = "customer_id", length = 120)
  private String customerId;

  @Column(name = "recipient_email", nullable = false, length = 255)
  private String recipientEmail;

  @Column(nullable = false, length = 20)
  private String channel;

  @Column(nullable = false, length = 255)
  private String subject;

  @Column(nullable = false, length = 2000)
  private String body;

  @Column(name = "correlation_id", length = 120)
  private String correlationId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getSourceEventId() {
    return sourceEventId;
  }

  public void setSourceEventId(UUID sourceEventId) {
    this.sourceEventId = sourceEventId;
  }

  public String getSourceEventType() {
    return sourceEventType;
  }

  public void setSourceEventType(String sourceEventType) {
    this.sourceEventType = sourceEventType;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public void setRecipientEmail(String recipientEmail) {
    this.recipientEmail = recipientEmail;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
