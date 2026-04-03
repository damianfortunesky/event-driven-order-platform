package com.eventdriven.inventory.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservation")
public class InventoryReservationJpaEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "payment_event_id", nullable = false, unique = true)
  private UUID paymentEventId;

  @Column(name = "order_id", nullable = false, unique = true)
  private UUID orderId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "failure_reason")
  private String failureReason;

  @Column(name = "correlation_id", nullable = false)
  private String correlationId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public UUID getId() { return id; }

  public void setId(UUID id) { this.id = id; }

  public UUID getPaymentEventId() { return paymentEventId; }

  public void setPaymentEventId(UUID paymentEventId) { this.paymentEventId = paymentEventId; }

  public UUID getOrderId() { return orderId; }

  public void setOrderId(UUID orderId) { this.orderId = orderId; }

  public String getStatus() { return status; }

  public void setStatus(String status) { this.status = status; }

  public String getFailureReason() { return failureReason; }

  public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

  public String getCorrelationId() { return correlationId; }

  public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

  public OffsetDateTime getCreatedAt() { return createdAt; }

  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
