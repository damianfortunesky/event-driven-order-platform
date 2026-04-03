package com.eventdriven.order.infrastructure.jpa.adapter;

import com.eventdriven.order.application.port.out.ProcessedEventRepositoryPort;
import com.eventdriven.order.infrastructure.jpa.entity.ProcessedEventJpaEntity;
import com.eventdriven.order.infrastructure.jpa.repository.ProcessedEventJpaRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessedEventRepositoryAdapter implements ProcessedEventRepositoryPort {

  private final ProcessedEventJpaRepository repository;

  @Override
  public boolean existsByEventId(UUID eventId) {
    return repository.existsById(eventId);
  }

  @Override
  public void save(UUID eventId, String eventType, Instant processedAt) {
    ProcessedEventJpaEntity entity = new ProcessedEventJpaEntity();
    entity.setEventId(eventId);
    entity.setEventType(eventType);
    entity.setProcessedAt(processedAt);
    repository.save(entity);
  }
}
