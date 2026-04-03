package com.eventdriven.notification.application;

import com.eventdriven.notification.domain.NotificationEventType;
import com.eventdriven.notification.infrastructure.jpa.entity.NotificationJpaEntity;
import com.eventdriven.notification.infrastructure.jpa.repository.NotificationJpaRepository;
import com.eventdriven.notification.infrastructure.kafka.model.FinalEventEnvelope;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationJpaRepository notificationRepository;
  private final NotificationMessageFactory messageFactory;

  @Transactional
  public void processFinalEvent(FinalEventEnvelope event) {
    if (!NotificationEventType.isSupported(event.eventType())) {
      log.warn("notification.event.unsupported eventId={} type={}", event.eventId(), event.eventType());
      return;
    }

    if (notificationRepository.existsBySourceEventIdAndSourceEventType(event.eventId(), event.eventType())) {
      log.info("notification.event.duplicate eventId={} type={}", event.eventId(), event.eventType());
      return;
    }

    NotificationDraft draft = messageFactory.create(event);

    NotificationJpaEntity notification = new NotificationJpaEntity();
    notification.setId(event.eventId());
    notification.setSourceEventId(event.eventId());
    notification.setSourceEventType(event.eventType());
    notification.setOrderId(draft.orderId());
    notification.setCustomerId(draft.customerId());
    notification.setRecipientEmail(draft.recipientEmail());
    notification.setChannel("EMAIL");
    notification.setSubject(draft.subject());
    notification.setBody(draft.body());
    notification.setCorrelationId(event.correlationId());
    notification.setCreatedAt(Instant.now());

    notificationRepository.save(notification);

    log.info(
        "notification.sent.simulated channel={} recipient={} orderId={} eventType={} subject={}",
        notification.getChannel(),
        notification.getRecipientEmail(),
        notification.getOrderId(),
        notification.getSourceEventType(),
        notification.getSubject()
    );
  }

  @Transactional(readOnly = true)
  public List<NotificationJpaEntity> findAll() {
    return notificationRepository.findAllByOrderByCreatedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<NotificationJpaEntity> findByOrderId(String orderId) {
    return notificationRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId);
  }
}
