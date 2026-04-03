package com.eventdriven.inventory.infrastructure.config;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {

  private final KafkaProperties kafkaProperties;

  @Value("${app.kafka.consumer.retry.max-attempts:3}")
  private int maxAttempts;

  @Value("${app.kafka.consumer.retry.initial-interval-ms:500}")
  private long initialInterval;

  @Value("${app.kafka.consumer.retry.multiplier:2.0}")
  private double multiplier;

  public KafkaConfig(KafkaProperties kafkaProperties) {
    this.kafkaProperties = kafkaProperties;
  }

  @Bean
  public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> props = kafkaProperties.buildConsumerProperties();
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ProducerFactory<String, String> producerFactory() {
    Map<String, Object> props = kafkaProperties.buildProducerProperties();
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory,
      KafkaTemplate<String, String> kafkaTemplate) {

    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaTemplate,
        (record, ex) -> new org.apache.kafka.common.TopicPartition(record.topic() + ".dlq", record.partition())
    );

    ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(maxAttempts - 1);
    backoff.setInitialInterval(initialInterval);
    backoff.setMultiplier(multiplier);

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backoff);

    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }
}
