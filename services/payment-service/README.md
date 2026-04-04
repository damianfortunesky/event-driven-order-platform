# payment-service

Microservicio Spring Boot 3 (Java 21) para el bounded context **Payment**.

## Responsabilidad
- Consumir eventos `OrderCreated` o `PaymentRequested`.
- Simular procesamiento de pago con reglas reproducibles.
- Persistir el resultado en PostgreSQL.
- Publicar `PaymentApproved` o `PaymentRejected`.

## Reglas de negocio (reproducibles)
1. **Rechazo por umbral**: si `totalAmount >= app.payment.approval-threshold` se rechaza.
2. **Rechazo pseudoaleatorio controlado**: si pasa el umbral, se calcula `hash(eventId) % 100`.
   - Si ese bucket es menor a `app.payment.rejection-percentage`, se rechaza para testing.
   - Al depender de `eventId`, el resultado es determinista.

## Idempotencia
- Se usa `event_id` con restricción `UNIQUE` en tabla `payments`.
- Ante eventos duplicados se detecta por `eventId` y se ignora reprocesamiento/publicación.

## Configuración Kafka
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${PAYMENT_KAFKA_CONSUMER_GROUP:payment-service-group}
      auto-offset-reset: earliest
    producer:
      acks: all
      retries: 3

app:
  kafka:
    topics:
      order-created: ${ORDER_CREATED_TOPIC:orders.order-created.v1}
      payment-requested: ${PAYMENT_REQUESTED_TOPIC:payments.payment-requested.v1}
      payment-approved: ${PAYMENT_APPROVED_TOPIC:payments.payment-approved.v1}
      payment-rejected: ${PAYMENT_REJECTED_TOPIC:payments.payment-rejected.v1}
    consumer:
      dlq-suffix: ${PAYMENT_CONSUMER_DLQ_SUFFIX:.dlq}
      retry:
        max-attempts: ${PAYMENT_CONSUMER_MAX_ATTEMPTS:4}
        initial-interval-ms: ${PAYMENT_CONSUMER_INITIAL_INTERVAL_MS:500}
        multiplier: ${PAYMENT_CONSUMER_RETRY_MULTIPLIER:2.0}
```

## Estrategia de error handling
- `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries`.
- `DeadLetterPublishingRecoverer` publica en `"<topic>${app.kafka.consumer.dlq-suffix}"` cuando agota retries.
- `InvalidPaymentEventException` se marca como **no-retryable** y va directo a DLQ (payload inválido o `eventType` no soportado).
- Excepciones transitorias de infraestructura se reintentan con backoff exponencial y luego se derivan a DLQ.

## Observabilidad
- Logs estructurados JSON (incluye `correlationId`).
- Métricas Micrometer/Actuator:
  - `payment.processing.duration` (timer)
  - `payment.processed.total{status=...}` (counter)
- Endpoint Prometheus: `/actuator/prometheus`.

## Migraciones Flyway
- `V1__create_payments_schema.sql`: crea tabla `payments` + índices.

## Ejecutar local
```bash
cd services/payment-service
mvn spring-boot:run
```

Variables clave:
- `KAFKA_BOOTSTRAP_SERVERS`
- `PAYMENT_DB_URL`
- `PAYMENT_DB_USERNAME`
- `PAYMENT_DB_PASSWORD`

## Docker
```bash
docker build -t payment-service:local services/payment-service
```

## Ejemplos de mensajes
### Entrada (`OrderCreated`)
```json
{
  "eventId": "24d0b7d8-bf9b-42e5-b43d-1fd9caa4d001",
  "eventType": "OrderCreated",
  "occurredAt": "2026-04-03T10:15:30Z",
  "correlationId": "corr-123",
  "payload": {
    "orderId": "b1592c13-25d7-4d95-aaca-8aaf4c4e2e10",
    "totalAmount": 49.90,
    "currency": "USD"
  }
}
```

### Salida (`PaymentApproved`)
```json
{
  "eventId": "728ef45d-6aa8-476f-95f3-c5fe20a8f913",
  "eventType": "PaymentApproved",
  "occurredAt": "2026-04-03T10:15:31Z",
  "correlationId": "corr-123",
  "payload": {
    "paymentId": "ef89f13e-5e3f-4fd1-8200-0bb606dc6e73",
    "orderId": "b1592c13-25d7-4d95-aaca-8aaf4c4e2e10",
    "totalAmount": 49.90,
    "status": "APPROVED",
    "reason": null
  }
}
```

### Salida (`PaymentRejected`)
```json
{
  "eventId": "728ef45d-6aa8-476f-95f3-c5fe20a8f914",
  "eventType": "PaymentRejected",
  "occurredAt": "2026-04-03T10:15:31Z",
  "correlationId": "corr-123",
  "payload": {
    "paymentId": "ef89f13e-5e3f-4fd1-8200-0bb606dc6e74",
    "orderId": "b1592c13-25d7-4d95-aaca-8aaf4c4e2e10",
    "totalAmount": 1200.00,
    "status": "REJECTED",
    "reason": "amount_above_threshold"
  }
}
```
