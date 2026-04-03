package com.eventdriven.order.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "processed_events")
public class ProcessedEventJpaEntity {
  @Id
  private UUID eventId;

  @Column(nullable = false, length = 120)
  private String eventType;

  @Column(nullable = false)
  private Instant processedAt;
}
