# Runbook: observabilidad EDA end-to-end

## 1) Configuración por servicio

Todos los servicios exponen:
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`
- logs JSON con `correlationId` via MDC.

### Orden de verificación rápida
1. `curl http://localhost:8081/actuator/health` (repetir por cada servicio/puerto).
2. `curl http://localhost:8081/actuator/info`.
3. `curl http://localhost:8081/actuator/prometheus | grep eda_`.

### Métricas instrumentadas
- `eda_events_consumed_total{service,event_type}`
- `eda_events_published_total{service,event_type,topic}`
- `eda_events_consume_errors_total{service,event_type}`
- `eda_events_retries_total{service,topic}`
- `eda_events_dlq_total{service,topic}`
- `eda_event_processing_latency_seconds{service,event_type,result,...}`

> Nota: retries y DLQ están instrumentados en los consumidores con `DefaultErrorHandler` (payment e inventory).

## 2) Ejemplos de logs JSON

```json
{"timestamp":"2026-04-04T12:10:45.331Z","level":"INFO","service":"order-service","correlationId":"1cf73501-7b7b-4a3f-95ec-4c6bbf979838","thread":"http-nio-8081-exec-2","logger":"OrderService","message":"order.created id=7f6... customerId=43d... correlationId=1cf..."}
```

```json
{"timestamp":"2026-04-04T12:10:46.219Z","level":"ERROR","service":"payment-service","correlationId":"1cf73501-7b7b-4a3f-95ec-4c6bbf979838","thread":"org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1","logger":"PaymentEventConsumer","message":"payment.event.consume.error message={...} error=Could not parse payment input event"}
```

## 3) Guía para analizar el flujo end-to-end

### Paso A - Tomar un `correlationId`
- Desde la request inicial (header `X-Correlation-Id`) o del evento `OrderCreated`.

### Paso B - Seguir el rastro en logs
- Buscar el mismo `correlationId` en logs de:
  1. `order-service`
  2. `payment-service`
  3. `inventory-service`
  4. `notification-service`

### Paso C - Validar evolución en métricas
- `eda_events_consumed_total` debe subir en el consumidor esperado.
- `eda_events_published_total` debe subir en el publicador siguiente.
- Si aumenta `eda_events_consume_errors_total`, revisar payload y esquema.
- Si aumentan `eda_events_retries_total` y `eda_events_dlq_total`, inspeccionar tópico `<topic>.dlq`.
- Revisar `eda_event_processing_latency_seconds` (p95) para detectar cuellos de botella.

### Paso D - Diagnóstico por síntoma
- **No se mueve el flujo**: health Kafka/DB degradado o consumer group detenido.
- **Muchos retries**: dependencia intermitente o validación frágil.
- **DLQ en aumento**: errores no recuperables o esquema incompatible.
- **Latencia alta**: lock en BD, saturación de broker o procesamiento sin paralelismo.

## 4) Queries PromQL útiles

```promql
sum(rate(eda_events_consumed_total[5m])) by (service, event_type)
```

```promql
sum(increase(eda_events_consume_errors_total[15m])) by (service, event_type)
```

```promql
sum(increase(eda_events_dlq_total[15m])) by (service, topic)
```

```promql
histogram_quantile(0.95, sum(rate(eda_event_processing_latency_seconds_bucket[5m])) by (le, service, event_type))
```
