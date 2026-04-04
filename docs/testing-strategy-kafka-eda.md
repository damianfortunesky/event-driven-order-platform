# Estrategia de testing para la plataforma Kafka + EDA

## 1) Estrategia general

### 1.1 Pirámide de tests propuesta

1. **Unitarios (rápidos, sin Kafka ni DB real)**
   - Dominio puro: reglas de negocio, decisiones de estado, validaciones.
   - Application services: comportamiento con puertos mockeados.
   - Objetivo: feedback < 5 minutos en cada PR.

2. **Integración por servicio (con infraestructura real aislada)**
   - Spring Boot + DB real (Testcontainers PostgreSQL).
   - Kafka real (Testcontainers Kafka/Redpanda) para producer/consumer.
   - Objetivo: validar wiring, serialización, offsets, transacciones e idempotencia por servicio.

3. **End-to-end (flujo completo multi-servicio)**
   - Levantar `order-service`, `payment-service`, `inventory-service`, `notification-service` + Kafka + Postgres.
   - Ejecutar escenarios reales y aserciones eventual-consistent.
   - Objetivo: confianza en la saga completa.

### 1.2 Matriz de cobertura funcional mínima

- **Happy path**: `OrderCreated -> PaymentApproved -> InventoryReserved -> OrderCompleted -> NotificationSent`.
- **Errores de negocio**: pago rechazado, stock insuficiente.
- **Errores técnicos**: payload inválido, timeouts transitorios, caída de dependencia.
- **No funcionales de resiliencia**:
  - idempotencia (deduplicación por `eventId` / `orderId`),
  - retries con backoff,
  - enrutamiento a DLQ al agotar intentos,
  - consistencia eventual en estados finales.

### 1.3 Convenciones de diseño de tests

- Naming: `given_when_then`.
- Usar builders/fixtures por evento (`OrderCreatedFixture`, etc.).
- Esperas asíncronas con `Awaitility` (timeouts cortos y deterministas).
- No usar `Thread.sleep` hardcodeado.
- En Kafka tests:
  - topic por test suite,
  - consumer group único por test para evitar interferencias,
  - limpiar estado (DB + topics) entre tests.

### 1.4 Dónde usar Testcontainers

Usar Testcontainers en:
- Integración de productores/consumidores Kafka.
- Integración de JPA/Flyway por servicio.
- End-to-end multi-servicio.

Evitar Testcontainers en:
- Unit tests puros (deben ser ultra rápidos).

Stack sugerido:
- `org.testcontainers:kafka`
- `org.testcontainers:postgresql`
- `org.testcontainers:junit-jupiter`

---

## 2) Casos de prueba concretos

## 2.1 Unitarios

### Order Service
- Crea orden válida -> estado inicial `PENDING`/`PENDING_PAYMENT` + evento publicado.
- Rechaza comando inválido (sin ítems, monto <= 0).
- Procesa evento final exitoso -> estado `COMPLETED`.
- Evento final duplicado -> no cambia estado ni genera efectos duplicados (idempotencia).

### Payment Service
- `PaymentDecisionEngine` aprueba/rechaza según reglas.
- `PaymentProcessingService` persiste transacción y publica `PaymentApproved` o `PaymentRejected`.
- Evento duplicado (`eventId` repetido) -> procesamiento único.

### Inventory Service
- Reserva stock suficiente -> emite `InventoryReserved`.
- Stock insuficiente -> emite `InventoryFailed`.
- Duplicado de `PaymentApproved` -> no duplica reserva.

### Notification Service
- Mapea tipo de evento final a mensaje correcto.
- Evita notificación duplicada con misma llave idempotente.

## 2.2 Integración por servicio

### order-service
- `POST /orders` persiste orden y publica evento en tópico correcto.
- Consumer de eventos finales actualiza estado en DB.
- Evento mal formado -> retries y luego DLQ.

### payment-service
- Consume `orders.order-created.v1` y publica en `payments.*`.
- Falla transitoria (simulada) -> retry N veces y luego éxito.
- Falla permanente (payload inválido) -> va a DLQ.

### inventory-service
- Consume `payments.payment-approved.v1`, reserva stock y publica resultado.
- Conflicto concurrente de stock -> garantiza consistencia de inventario.
- Idempotencia en tabla inbox/processed-events.

### notification-service
- Consume eventos finales y persiste notificación.
- Error de serialización permanente -> DLQ.

## 2.3 End-to-end

### Caso E2E éxito
1. Crear orden por API.
2. Verificar emisión de `OrderCreated`.
3. Verificar `PaymentApproved`.
4. Verificar `InventoryReserved`.
5. Verificar `OrderCompleted`.
6. Verificar notificación persistida.
7. Aserción final: consistencia eventual (estado final en todas las DB coherente).

### Caso E2E error pago
1. Crear orden con condición de rechazo de pago.
2. Verificar `PaymentRejected`.
3. Verificar `OrderFailed`.
4. Verificar notificación de fallo.

### Caso E2E con DLQ
1. Inyectar evento inválido en tópico de entrada de `payment-service`.
2. Verificar retries configurados.
3. Verificar mensaje final en `<topic>.DLT`.

---

## 3) Ejemplos de tests por servicio

> Nota: el repo ya tiene base de tests de unidad e integración en servicios clave (order, payment, inventory, notification). Se recomienda extenderlos con matrices de resiliencia (retry/DLQ/idempotencia).

## 3.1 `order-service` (ejemplo)

```java
@Test
void givenDuplicatedFinalEvent_whenProcessed_thenOrderStateIsUpdatedOnce() {
    // given
    var event = finalEvent("evt-1", "OrderCompleted", orderId);

    // when
    consumer.consume(event);
    consumer.consume(event); // duplicado

    // then
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
        var order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(COMPLETED);
        assertThat(processedEventRepository.countByEventId("evt-1")).isEqualTo(1);
    });
}
```

## 3.2 `payment-service` (retry + DLQ)

```java
@Test
void givenPermanentDeserializationError_whenConsumed_thenMessageGoesToDLQ() {
    // given
    sendRawInvalidMessage("orders.order-created.v1", "{bad-json}");

    // when / then
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
        var dlqRecords = readFrom("orders.order-created.v1.DLT");
        assertThat(dlqRecords).hasSize(1);
        assertThat(dlqRecords.getFirst().headers())
            .containsKey("x-exception-message");
    });
}
```

## 3.3 `inventory-service` (idempotencia)

```java
@Test
void givenSamePaymentApprovedTwice_whenProcessed_thenCreatesSingleReservation() {
    var event = paymentApproved("evt-777", orderId, items);

    consumer.consume(event);
    consumer.consume(event);

    await().untilAsserted(() -> {
        assertThat(inboxEventRepository.countByEventId("evt-777")).isEqualTo(1);
        assertThat(inventoryReservationRepository.countByOrderId(orderId)).isEqualTo(1);
    });
}
```

## 3.4 `notification-service` (consistencia eventual)

```java
@Test
void givenOrderCompletedEvent_whenConsumed_thenNotificationIsPersistedEventually() {
    sendEvent("orders.order-completed.v1", orderCompleted(orderId));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
        var notifications = notificationRepository.findByOrderId(orderId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getStatus()).isEqualTo("SENT");
    });
}
```

---

## 4) Ejemplo de test end-to-end del flujo completo

```java
@Testcontainers
@SpringBootTest
class OrderFlowE2ETest {

    @Container static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");
    @Container static PostgreSQLContainer<?> orderDb = new PostgreSQLContainer<>("postgres:16");
    @Container static PostgreSQLContainer<?> paymentDb = new PostgreSQLContainer<>("postgres:16");
    @Container static PostgreSQLContainer<?> inventoryDb = new PostgreSQLContainer<>("postgres:16");
    @Container static PostgreSQLContainer<?> notificationDb = new PostgreSQLContainer<>("postgres:16");

    @Test
    void shouldCompleteOrderSagaSuccessfully() {
        // 1) Crear orden
        var orderId = createOrderViaApi();

        // 2) Aserción de consistencia eventual multi-servicio
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(orderStatus(orderId)).isEqualTo("COMPLETED");
            assertThat(paymentStatus(orderId)).isEqualTo("APPROVED");
            assertThat(inventoryStatus(orderId)).isEqualTo("RESERVED");
            assertThat(notificationStatus(orderId)).isEqualTo("SENT");
        });

        // 3) Idempotencia: reinyectar último evento
        replayLastEventFor(orderId);

        await().untilAsserted(() -> {
            assertThat(countNotifications(orderId)).isEqualTo(1);
            assertThat(countReservations(orderId)).isEqualTo(1);
        });
    }
}
```

### Variante E2E de error con DLQ

- Inyectar mensaje inválido en `orders.order-created.v1`.
- Verificar N retries en métricas/log headers (`deliveryAttempt`).
- Verificar presencia final en `orders.order-created.v1.DLT`.
- Verificar que la saga no marca orden como `COMPLETED`.

---

## 5) Recomendaciones para correr tests en CI

## 5.1 Pipeline por etapas

1. **Stage 1 (rápido, bloqueante)**
   - `mvn -q -DskipITs test` (unit tests).
2. **Stage 2 (integración por servicio)**
   - `mvn -q -pl services/order-service,services/payment-service,services/inventory-service,services/notification-service verify`.
3. **Stage 3 (E2E)**
   - suite separada (puede correr en paralelo por escenarios).
4. **Stage 4 (contract checks opcional)**
   - validación de schemas JSON + compatibilidad backward.

## 5.2 Reglas de estabilidad

- Reintentar job de CI solo para fallas de infraestructura (no para aserciones).
- Timeouts explícitos por suite (unit < 5m, integración < 15m, e2e < 20m).
- Ejecutar tests Kafka con tópicos únicos por build (`${CI_PIPELINE_ID}`).
- Publicar artifacts en fallas:
  - logs de servicios,
  - logs de contenedores,
  - mensajes de DLQ,
  - reporte JUnit XML.

## 5.3 Paralelización recomendada

- Unit tests: paralelos por módulo.
- Integración: paralelos por servicio (containers aislados).
- E2E: serial por escenario crítico para reducir flaky behavior.

## 5.4 Gates de calidad

- Cobertura mínima sugerida:
  - dominio/aplicación: 80%
  - adaptadores críticos Kafka: 70%
- Deben pasar obligatoriamente:
  - al menos 1 test de idempotencia por consumidor,
  - al menos 1 test DLQ por servicio consumidor,
  - al menos 1 E2E de éxito + 1 E2E de error.

## 5.5 Métricas útiles para observar en CI

- `consumer_lag` por tópico.
- `retry_attempts_total`.
- `dlq_messages_total`.
- latencia `event -> estado final` (p95/p99).

---

## Plan de adopción incremental (sugerido)

1. Consolidar unit tests existentes y completar casos de error.
2. Homogeneizar base de Testcontainers por servicio (clase abstracta compartida).
3. Implementar tests de retry + DLQ faltantes.
4. Crear suite E2E nightly y luego mover subset crítico a cada PR.
