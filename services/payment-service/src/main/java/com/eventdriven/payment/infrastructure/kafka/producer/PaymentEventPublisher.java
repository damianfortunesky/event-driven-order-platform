package com.eventdriven.payment.infrastructure.kafka.producer;

import com.eventdriven.payment.infrastructure.kafka.model.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;

  @Value("${app.kafka.topics.payment-approved}")
  private String paymentApprovedTopic;

  @Value("${app.kafka.topics.payment-rejected}")
  private String paymentRejectedTopic;

  public void publish(EventEnvelope event) {
    String topic = "PaymentApproved".equals(event.eventType()) ? paymentApprovedTopic : paymentRejectedTopic;
    try {
      String message = objectMapper.writeValueAsString(event);
      kafkaTemplate.send(topic, event.eventId().toString(), message);
      meterRegistry.counter("eda.events.published.total", "event_type", event.eventType(), "topic", topic).increment();
      log.info("payment.event.published eventId={} topic={} type={}", event.eventId(), topic, event.eventType());
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Could not serialize payment event", ex);
    }
  }
}
