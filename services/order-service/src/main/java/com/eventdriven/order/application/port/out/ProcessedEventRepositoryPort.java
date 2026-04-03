package com.eventdriven.order.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedEventRepositoryPort {
  boolean existsByEventId(UUID eventId);

  void save(UUID eventId, String eventType, Instant processedAt);
}
