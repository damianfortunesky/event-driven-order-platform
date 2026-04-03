package com.eventdriven.payment.application;

import com.eventdriven.payment.domain.PaymentDecision;
import com.eventdriven.payment.domain.PaymentDecisionEngine;
import com.eventdriven.payment.domain.PaymentStatus;
import com.eventdriven.payment.infrastructure.jpa.entity.PaymentJpaEntity;
import com.eventdriven.payment.infrastructure.jpa.repository.PaymentJpaRepository;
import com.eventdriven.payment.infrastructure.kafka.model.EventEnvelope;
import com.eventdriven.payment.infrastructure.kafka.model.PaymentProcessedPayload;
import com.eventdriven.payment.infrastructure.kafka.model.PaymentRequestedPayload;
import com.eventdriven.payment.infrastructure.kafka.producer.PaymentEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

  private final PaymentJpaRepository paymentRepository;
  private final PaymentDecisionEngine decisionEngine;
  private final PaymentEventPublisher eventPublisher;
  private final MeterRegistry meterRegistry;

  @Transactional
  public void process(EventEnvelope incomingEvent) {
    Timer.Sample timer = Timer.start(meterRegistry);
    UUID eventId = incomingEvent.eventId();

    paymentRepository.findByEventId(eventId).ifPresentOrElse(
        existing -> log.info("payment.event.duplicate eventId={} paymentId={}", eventId, existing.getId()),
        () -> processNewEvent(incomingEvent)
    );

    timer.stop(Timer.builder("payment.processing.duration").register(meterRegistry));
  }

  private void processNewEvent(EventEnvelope incomingEvent) {
    PaymentRequestedPayload payload = (PaymentRequestedPayload) incomingEvent.payload();
    PaymentDecision decision = decisionEngine.evaluate(incomingEvent.eventId(), payload.totalAmount());

    PaymentJpaEntity entity = new PaymentJpaEntity();
    entity.setEventId(incomingEvent.eventId());
    entity.setOrderId(payload.orderId());
    entity.setTotalAmount(payload.totalAmount());
    entity.setCurrency(payload.currency());
    entity.setStatus(decision.status());
    entity.setFailureReason(decision.reason());
    entity.setCorrelationId(incomingEvent.correlationId());
    entity.setCreatedAt(OffsetDateTime.now());

    PaymentJpaEntity saved = paymentRepository.save(entity);

    EventEnvelope outEvent = new EventEnvelope(
        UUID.randomUUID(),
        decision.status() == PaymentStatus.APPROVED ? "PaymentApproved" : "PaymentRejected",
        OffsetDateTime.now(),
        incomingEvent.correlationId(),
        new PaymentProcessedPayload(
            saved.getId(),
            saved.getOrderId(),
            saved.getTotalAmount(),
            saved.getStatus().name(),
            saved.getFailureReason()
        )
    );

    eventPublisher.publish(outEvent);
    Counter.builder("payment.processed.total")
        .tag("status", saved.getStatus().name())
        .register(meterRegistry)
        .increment();

    log.info(
        "payment.event.processed eventId={} paymentId={} orderId={} status={} reason={}",
        incomingEvent.eventId(), saved.getId(), saved.getOrderId(), saved.getStatus(), saved.getFailureReason()
    );
  }
}
