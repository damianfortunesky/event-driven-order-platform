package com.eventdriven.payment.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventdriven.payment.infrastructure.jpa.repository.PaymentJpaRepository;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    "orders.order-created.v1",
    "payments.payment-requested.v1",
    "payments.payment-approved.v1",
    "payments.payment-rejected.v1",
    "orders.order-created.v1.dlq"
})
class PaymentEventConsumerIntegrationTest {

  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private PaymentJpaRepository paymentRepository;

  private Consumer<String, String> consumer;

  @BeforeEach
  void setup() {
    Map<String, Object> props = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
        .createConsumer();
    embeddedKafkaBroker.consumeFromAnEmbeddedTopic(
        consumer,
        "payments.payment-approved.v1",
        "payments.payment-rejected.v1",
        "orders.order-created.v1.dlq"
    );
  }

  @AfterEach
  void tearDown() {
    consumer.close();
    paymentRepository.deleteAll();
  }

  @Test
  void shouldConsumePersistAndPublishApprovedEventFromOrderCreated() {
    UUID eventId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    String message = """
        {
          "eventId": "%s",
          "eventType": "OrderCreated",
          "occurredAt": "2026-04-03T10:15:30Z",
          "correlationId": "corr-int-1",
          "payload": {
            "orderId": "%s",
            "totalAmount": 10.00,
            "currency": "USD"
          }
        }
        """.formatted(eventId, orderId);

    kafkaTemplate.send("orders.order-created.v1", orderId.toString(), message);

    ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
        consumer,
        "payments.payment-approved.v1",
        Duration.ofSeconds(10)
    );

    assertThat(record.value()).contains("PaymentApproved");
    assertThat(paymentRepository.findByEventId(eventId)).isPresent();

    kafkaTemplate.send("orders.order-created.v1", orderId.toString(), message);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
    assertThat(paymentRepository.count()).isEqualTo(1);
  }

  @Test
  void shouldConsumePaymentRequestedTopic() {
    UUID eventId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    String message = """
        {
          "eventId": "%s",
          "eventType": "PaymentRequested",
          "occurredAt": "2026-04-03T10:15:30Z",
          "correlationId": "corr-int-2",
          "payload": {
            "orderId": "%s",
            "totalAmount": 25.00,
            "currency": "USD"
          }
        }
        """.formatted(eventId, orderId);

    kafkaTemplate.send("payments.payment-requested.v1", orderId.toString(), message);

    ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
        consumer,
        "payments.payment-approved.v1",
        Duration.ofSeconds(10)
    );

    assertThat(record.value()).contains("PaymentApproved");
    assertThat(paymentRepository.findByEventId(eventId)).isPresent();
  }

  @Test
  void shouldSendInvalidEventToDlqWithoutRetries() {
    String invalidMessage = """
        {
          "eventId": "a19d5ec7-3f5b-44d1-95a7-684ce2f4a01a",
          "eventType": "UnsupportedEvent",
          "occurredAt": "2026-04-03T10:15:30Z",
          "correlationId": "corr-int-3",
          "payload": {
            "orderId": "11ae1ea5-ecb6-4228-8f7d-3d15fe9fa975",
            "totalAmount": 25.00,
            "currency": "USD"
          }
        }
        """;

    kafkaTemplate.send("orders.order-created.v1", "key", invalidMessage);

    ConsumerRecord<String, String> dlqRecord = KafkaTestUtils.getSingleRecord(
        consumer,
        "orders.order-created.v1.dlq",
        Duration.ofSeconds(10)
    );

    assertThat(dlqRecord.value()).contains("UnsupportedEvent");
    assertThat(paymentRepository.count()).isZero();
  }
}
