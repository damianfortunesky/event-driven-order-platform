package com.eventdriven.payment.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventdriven.payment.domain.PaymentDecision;
import com.eventdriven.payment.domain.PaymentDecisionEngine;
import com.eventdriven.payment.domain.PaymentStatus;
import com.eventdriven.payment.infrastructure.jpa.entity.PaymentJpaEntity;
import com.eventdriven.payment.infrastructure.jpa.repository.PaymentJpaRepository;
import com.eventdriven.payment.infrastructure.kafka.model.EventEnvelope;
import com.eventdriven.payment.infrastructure.kafka.model.PaymentRequestedPayload;
import com.eventdriven.payment.infrastructure.kafka.producer.PaymentEventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaymentProcessingServiceTest {

  private PaymentJpaRepository repository;
  private PaymentEventPublisher publisher;
  private PaymentDecisionEngine decisionEngine;
  private PaymentProcessingService service;

  @BeforeEach
  void setUp() {
    repository = Mockito.mock(PaymentJpaRepository.class);
    publisher = Mockito.mock(PaymentEventPublisher.class);
    decisionEngine = Mockito.mock(PaymentDecisionEngine.class);
    service = new PaymentProcessingService(repository, decisionEngine, publisher, new SimpleMeterRegistry());
  }

  @Test
  void shouldSkipDuplicateEvent() {
    UUID eventId = UUID.randomUUID();
    PaymentJpaEntity existing = new PaymentJpaEntity();
    existing.setId(UUID.randomUUID());
    when(repository.findByEventId(eventId)).thenReturn(Optional.of(existing));

    service.process(new EventEnvelope(
        eventId,
        "PaymentRequested",
        OffsetDateTime.now(),
        "corr-1",
        new PaymentRequestedPayload(UUID.randomUUID(), BigDecimal.TEN, "USD")
    ));

    verify(repository, never()).save(any());
    verify(publisher, never()).publish(any());
  }

  @Test
  void shouldPersistAndPublishForNewEvent() {
    UUID eventId = UUID.randomUUID();
    when(repository.findByEventId(eventId)).thenReturn(Optional.empty());
    when(decisionEngine.evaluate(any(), any())).thenReturn(PaymentDecision.approved());
    when(repository.save(any())).thenAnswer(invocation -> {
      PaymentJpaEntity e = invocation.getArgument(0);
      e.setId(UUID.randomUUID());
      e.setStatus(PaymentStatus.APPROVED);
      return e;
    });

    service.process(new EventEnvelope(
        eventId,
        "PaymentRequested",
        OffsetDateTime.now(),
        "corr-1",
        new PaymentRequestedPayload(UUID.randomUUID(), BigDecimal.TEN, "USD")
    ));

    verify(repository).save(any());
    verify(publisher).publish(any());
  }
}
