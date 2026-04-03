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
    "payments.payment-approved.v1",
    "payments.payment-rejected.v1"
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
    embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "payments.payment-approved.v1", "payments.payment-rejected.v1");
  }

  @AfterEach
  void tearDown() {
    consumer.close();
    paymentRepository.deleteAll();
  }

  @Test
  void shouldConsumePersistAndPublishApprovedEvent() {
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
}
