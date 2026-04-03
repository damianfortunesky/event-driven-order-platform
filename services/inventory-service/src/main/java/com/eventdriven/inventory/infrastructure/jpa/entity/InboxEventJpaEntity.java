package com.eventdriven.inventory.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_inbox_event")
public class InboxEventJpaEntity {

  @Id
  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "correlation_id", nullable = false)
  private String correlationId;

  @Column(name = "processed_at", nullable = false)
  private OffsetDateTime processedAt;

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public OffsetDateTime getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(OffsetDateTime processedAt) {
    this.processedAt = processedAt;
  }
}
