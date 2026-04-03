package com.eventdriven.notification.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventdriven.notification.infrastructure.jpa.repository.NotificationJpaRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    "orders.order-confirmed.v1",
    "orders.order-cancelled.v1",
    "payments.payment-rejected.v1",
    "inventory.inventory-failed.v1"
})
class FinalEventsConsumerIntegrationTest {

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private NotificationJpaRepository notificationRepository;

  @AfterEach
  void tearDown() {
    notificationRepository.deleteAll();
  }

  @Test
  void shouldConsumeFinalEventAndPersistNotificationIdempotently() throws Exception {
    UUID eventId = UUID.randomUUID();
    String orderId = "ord-int-1";

    String message = """
        {
          "eventId": "%s",
          "eventType": "OrderCancelled",
          "occurredAt": "2026-04-03T10:15:30Z",
          "correlationId": "corr-int-1",
          "payload": {
            "orderId": "%s",
            "customerId": "cus-int-1",
            "reasonCode": "PAYMENT_REJECTED"
          }
        }
        """.formatted(eventId, orderId);

    kafkaTemplate.send("orders.order-cancelled.v1", orderId, message).get();
    Thread.sleep(1000);

    assertThat(notificationRepository.count()).isEqualTo(1);
    assertThat(notificationRepository.findAll().getFirst().getSubject()).isEqualTo("Tu orden fue cancelada");

    kafkaTemplate.send("orders.order-cancelled.v1", orderId, message).get();
    Thread.sleep(1000);

    assertThat(notificationRepository.count()).isEqualTo(1);
  }
}
