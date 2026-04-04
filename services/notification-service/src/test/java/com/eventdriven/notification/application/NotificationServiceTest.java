package com.eventdriven.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventdriven.notification.infrastructure.jpa.entity.NotificationJpaEntity;
import com.eventdriven.notification.infrastructure.jpa.repository.NotificationJpaRepository;
import com.eventdriven.notification.infrastructure.kafka.model.FinalEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationJpaRepository notificationRepository;

  private final NotificationMessageFactory messageFactory = new NotificationMessageFactory();

  @InjectMocks
  private NotificationService notificationService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    notificationService = new NotificationService(notificationRepository, messageFactory);
  }

  @Test
  void shouldPersistNotificationForSupportedEvent() {
    FinalEventEnvelope event = new FinalEventEnvelope(
        UUID.randomUUID(),
        "OrderConfirmed",
        "corr-unit-1",
        objectMapper.createObjectNode()
            .put("orderId", "ord-1")
            .put("customerId", "cus-1")
            .put("customerEmail", "user@example.com")
    );

    when(notificationRepository.existsBySourceEventIdAndSourceEventType(event.eventId(), event.eventType()))
        .thenReturn(false);

    notificationService.processFinalEvent(event);

    ArgumentCaptor<NotificationJpaEntity> captor = ArgumentCaptor.forClass(NotificationJpaEntity.class);
    verify(notificationRepository).save(captor.capture());

    NotificationJpaEntity saved = captor.getValue();
    assertThat(saved.getSourceEventType()).isEqualTo("OrderConfirmed");
    assertThat(saved.getOrderId()).isEqualTo("ord-1");
    assertThat(saved.getRecipientEmail()).isEqualTo("user@example.com");
    assertThat(saved.getChannel()).isEqualTo("EMAIL");
  }

  @Test
  void shouldIgnoreDuplicateOnRaceCondition() {
    FinalEventEnvelope event = new FinalEventEnvelope(
        UUID.randomUUID(),
        "PaymentRejected",
        "corr-unit-2",
        objectMapper.createObjectNode()
            .put("orderId", "ord-2")
            .put("customerId", "cus-2")
            .put("reasonCode", "DECLINED")
    );

    when(notificationRepository.existsBySourceEventIdAndSourceEventType(event.eventId(), event.eventType()))
        .thenReturn(false);
    when(notificationRepository.save(any(NotificationJpaEntity.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    notificationService.processFinalEvent(event);

    verify(notificationRepository).save(any(NotificationJpaEntity.class));
  }

  @Test
  void shouldSkipUnsupportedEvent() {
    FinalEventEnvelope event = new FinalEventEnvelope(
        UUID.randomUUID(),
        "OrderCreated",
        "corr-unit-3",
        objectMapper.createObjectNode().put("orderId", "ord-3")
    );

    notificationService.processFinalEvent(event);

    verify(notificationRepository, never()).save(any(NotificationJpaEntity.class));
  }
}
