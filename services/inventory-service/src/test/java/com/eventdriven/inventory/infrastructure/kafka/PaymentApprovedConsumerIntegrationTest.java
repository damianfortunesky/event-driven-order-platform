package com.eventdriven.inventory.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventdriven.inventory.infrastructure.jpa.entity.InventoryStockJpaEntity;
import com.eventdriven.inventory.infrastructure.jpa.repository.InboxEventJpaRepository;
import com.eventdriven.inventory.infrastructure.jpa.repository.InventoryReservationJpaRepository;
import com.eventdriven.inventory.infrastructure.jpa.repository.InventoryStockJpaRepository;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
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
    "payments.payment-approved.v1",
    "inventory.inventory-reserved.v1",
    "inventory.inventory-failed.v1"
})
class PaymentApprovedConsumerIntegrationTest {

  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private InventoryStockJpaRepository stockRepository;

  @Autowired
  private InventoryReservationJpaRepository reservationRepository;

  @Autowired
  private InboxEventJpaRepository inboxRepository;

  private Consumer<String, String> consumer;

  @BeforeEach
  void setup() {
    Map<String, Object> props = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
        .createConsumer();
    embeddedKafkaBroker.consumeFromAnEmbeddedTopic(
        consumer,
        "inventory.inventory-reserved.v1",
        "inventory.inventory-failed.v1"
    );
  }

  @AfterEach
  void teardown() {
    consumer.close();
    reservationRepository.deleteAll();
    inboxRepository.deleteAll();
    resetStock("11111111-1111-1111-1111-111111111111", 10);
    resetStock("22222222-2222-2222-2222-222222222222", 3);
    resetStock("33333333-3333-3333-3333-333333333333", 0);
  }

  @Test
  void shouldPublishInventoryFailedWhenStockIsInsufficient() {
    String message = paymentApprovedEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "22222222-2222-2222-2222-222222222222",
        99
    );

    kafkaTemplate.send("payments.payment-approved.v1", "k1", message);

    ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
        consumer,
        "inventory.inventory-failed.v1",
        Duration.ofSeconds(10)
    );

    assertThat(record.value()).contains("InventoryFailed");
    assertThat(record.value()).contains("INSUFFICIENT_STOCK");

    Optional<InventoryStockJpaEntity> stock = stockRepository.findById(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    assertThat(stock).isPresent();
    assertThat(stock.get().getAvailableQuantity()).isEqualTo(3);
  }

  @Test
  void shouldNotDuplicateReservationWhenEventArrivesTwice() throws Exception {
    UUID eventId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();

    String message = paymentApprovedEvent(eventId, orderId, "11111111-1111-1111-1111-111111111111", 2);

    kafkaTemplate.send("payments.payment-approved.v1", "k1", message);
    ConsumerRecord<String, String> first = KafkaTestUtils.getSingleRecord(
        consumer,
        "inventory.inventory-reserved.v1",
        Duration.ofSeconds(10)
    );
    assertThat(first.value()).contains("InventoryReserved");

    kafkaTemplate.send("payments.payment-approved.v1", "k1", message);
    ConsumerRecord<String, String> second = KafkaTestUtils.getSingleRecord(
        consumer,
        "inventory.inventory-reserved.v1",
        Duration.ofSeconds(10)
    );
    assertThat(second.value()).contains("InventoryReserved");

    Thread.sleep(700);

    assertThat(reservationRepository.count()).isEqualTo(1);
    assertThat(inboxRepository.count()).isEqualTo(1);

    InventoryStockJpaEntity stock = stockRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111")).orElseThrow();
    assertThat(stock.getAvailableQuantity()).isEqualTo(8);
  }

  private void resetStock(String productId, long quantity) {
    InventoryStockJpaEntity stock = stockRepository.findById(UUID.fromString(productId)).orElseThrow();
    stock.setAvailableQuantity(quantity);
    stockRepository.save(stock);
  }

  private String paymentApprovedEvent(UUID eventId, UUID orderId, String productId, long quantity) {
    return """
        {
          "eventId": "%s",
          "eventType": "PaymentApproved",
          "occurredAt": "2026-04-03T10:15:30Z",
          "correlationId": "corr-it-1",
          "payload": {
            "paymentId": "%s",
            "orderId": "%s",
            "status": "APPROVED",
            "items": [
              {
                "productId": "%s",
                "quantity": %d
              }
            ]
          }
        }
        """.formatted(eventId, UUID.randomUUID(), orderId, productId, quantity);
  }
}
