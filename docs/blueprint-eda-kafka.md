# Blueprint técnico — Plataforma Event-Driven de Procesamiento de Órdenes

## 0) Objetivo del blueprint
Diseñar una plataforma didáctica pero profesional para aprender **Apache Kafka + Event-Driven Architecture (EDA)** end-to-end con:
- Java 21 + Spring Boot
- Microservicios simples con enfoque hexagonal
- PostgreSQL por servicio
- Docker Compose para desarrollo local completo
- Observabilidad con Actuator, Prometheus, Grafana y logs estructurados
- Buenas prácticas de resiliencia: retries, DLQ, idempotencia, correlation/trace IDs
- Ruta de deployment hacia Kubernetes

> Principio guía: **simplicidad correcta sobre sobreingeniería**, sin perder prácticas reales de producción.

---

## 1) Propuesta completa de arquitectura

### 1.1 Bounded Contexts
1. **Ordering** (`order-service`)
   - Dueño del ciclo de vida de la orden.
   - Expone API REST para creación y consulta.
   - Publica evento inicial y reacciona a resultados finales para cerrar estado.

2. **Payments** (`payment-service`)
   - Dueño del intento y resultado de pago.
   - Consume órdenes creadas, ejecuta pago simulado y emite resultado.

3. **Inventory** (`inventory-service`)
   - Dueño del stock reservado/liberado.
   - Consume pagos aprobados y emite resultado de reserva.

4. **Notifications** (`notification-service`)
   - Dueño del registro/envío de notificaciones.
   - Consume eventos finales (éxito/fracaso) y registra delivery.

5. **Shipping (Etapa 2)** (`shipping-service`)
   - Dueño de preparación/envío logístico.
   - Consume `InventoryReserved` y emite `ShipmentCreated`.

### 1.2 Estilo de interacción
- **Síncrono REST**: solo para comandos de entrada (crear orden, consultar estado).
- **Asíncrono Kafka**: para todo flujo cross-service de negocio.
- **Sin llamadas REST entre microservicios** para evitar acoplamiento temporal.

### 1.3 Patrón de consistencia
- **Eventual consistency** con orquestación ligera desde `order-service` (state machine interna de orden).
- Estados intermedios explícitos: `PENDING_PAYMENT`, `PAYMENT_APPROVED`, `INVENTORY_RESERVED`, `COMPLETED`, `FAILED`.

### 1.4 Patrón de publicación confiable
- **Transactional Outbox** por servicio productor de eventos.
  - Persistir estado + outbox en la misma transacción de DB.
  - Publicar outbox a Kafka con proceso interno scheduler/relay.
  - Marcar outbox como enviado tras confirmación.
- Beneficio didáctico: evita dual-write inconsistente.

### 1.5 Diseño hexagonal mínimo por servicio
- `domain`: entidades, value objects, reglas de negocio.
- `application`: casos de uso, puertos de entrada/salida.
- `infrastructure`: adapters REST, Kafka, JPA, config, observabilidad.

---

## 2) Estructura de carpetas del monorepo

```text
event-driven-order-platform/
├─ README.md
├─ docs/
│  ├─ blueprint-eda-kafka.md
│  ├─ adr/
│  │  ├─ 0001-topic-naming.md
│  │  ├─ 0002-outbox-pattern.md
│  │  └─ 0003-retry-dlq-strategy.md
│  ├─ diagrams/
│  │  ├─ context-map.mmd
│  │  ├─ event-flow-sequence.mmd
│  │  └─ deployment-k8s.mmd
│  └─ runbooks/
│     ├─ local-dev.md
│     ├─ kafka-troubleshooting.md
│     └─ incident-dlq-reprocessing.md
├─ build-logic/                       # convenciones gradle compartidas
├─ gradle/
├─ gradlew
├─ settings.gradle.kts
├─ docker/
│  ├─ compose/
│  │  ├─ docker-compose.local.yml
│  │  └─ docker-compose.monitoring.yml
│  ├─ kafka/
│  │  └─ create-topics.sh
│  └─ grafana/
│     ├─ dashboards/
│     └─ provisioning/
├─ k8s/
│  ├─ base/
│  │  ├─ namespaces.yaml
│  │  ├─ kafka-topics.yaml
│  │  ├─ postgres-*.yaml
│  │  ├─ order-service.yaml
│  │  ├─ payment-service.yaml
│  │  ├─ inventory-service.yaml
│  │  └─ notification-service.yaml
│  └─ overlays/
│     ├─ local-kind/
│     └─ cloud-dev/
├─ shared/
│  ├─ event-contracts/                # Avro/JSON schemas versionados
│  ├─ observability-starter/          # logging, tracing, metrics comunes
│  └─ test-utils/                     # testcontainers, fixtures, builders
└─ services/
   ├─ order-service/
   │  ├─ src/main/java/.../domain
   │  ├─ src/main/java/.../application
   │  ├─ src/main/java/.../infrastructure
   │  ├─ src/main/resources/
   │  │  ├─ application.yml
   │  │  └─ db/migration
   │  └─ src/test/java/...
   ├─ payment-service/
   ├─ inventory-service/
   ├─ notification-service/
   └─ shipping-service/               # etapa 2
```

---

## 3) Servicios y responsabilidad

### `order-service`
- API REST:
  - `POST /api/v1/orders`
  - `GET /api/v1/orders/{orderId}`
- Persiste orden + outbox de `OrderCreated`.
- Consume:
  - `PaymentApproved`, `PaymentRejected`
  - `InventoryReserved`, `InventoryFailed`
- Mantiene state machine y estado final.

### `payment-service`
- Consume `OrderCreated`.
- Ejecuta pago simulado (con porcentaje de fallo configurable).
- Publica `PaymentApproved` o `PaymentRejected`.
- Idempotencia por `orderId + eventId`.

### `inventory-service`
- Consume `PaymentApproved`.
- Reserva stock por ítem en DB local.
- Publica `InventoryReserved` o `InventoryFailed`.
- Maneja concurrencia con lock optimista o `SELECT ... FOR UPDATE`.

### `notification-service`
- Consume eventos finales (`OrderCompleted`, `OrderFailed`, o combinaciones equivalentes).
- Registra notificación enviada (email/sms simulado).
- No afecta flujo transaccional principal (best effort con retry + DLQ).

### `shipping-service` (opcional etapa 2)
- Consume `InventoryReserved`.
- Crea envío y publica `ShipmentCreated` / `ShipmentFailed`.

---

## 4) Lista de eventos Kafka y payloads sugeridos

## 4.1 Convención de nombres
- Topic: `orders.order-created.v1`, `payments.payment-approved.v1`, etc.
- Event name en payload: `OrderCreated`.
- Versionado explícito: `v1` en topic y/o schema registry subject.

## 4.2 Envelope base (común)

```json
{
  "eventId": "uuid",
  "eventType": "OrderCreated",
  "eventVersion": 1,
  "occurredAt": "2026-04-03T12:00:00Z",
  "source": "order-service",
  "correlationId": "uuid",
  "traceId": "hex-trace-id",
  "partitionKey": "orderId",
  "payload": {}
}
```

## 4.3 Eventos de negocio

1. **OrderCreated**  
   Topic: `orders.order-created.v1`  
   Key: `orderId`
   Payload sugerido:
   - `orderId`, `customerId`, `currency`, `totalAmount`, `items[]`, `createdAt`

2. **PaymentApproved**  
   Topic: `payments.payment-approved.v1`  
   Key: `orderId`
   Payload:
   - `orderId`, `paymentId`, `approvedAt`, `amount`, `provider`, `authorizationCode`

3. **PaymentRejected**  
   Topic: `payments.payment-rejected.v1`  
   Key: `orderId`
   Payload:
   - `orderId`, `paymentId`, `rejectedAt`, `reasonCode`, `reasonMessage`

4. **InventoryReserved**  
   Topic: `inventory.inventory-reserved.v1`  
   Key: `orderId`
   Payload:
   - `orderId`, `reservationId`, `reservedAt`, `items[]`

5. **InventoryFailed**  
   Topic: `inventory.inventory-failed.v1`  
   Key: `orderId`
   Payload:
   - `orderId`, `failedAt`, `reasonCode` (`INSUFFICIENT_STOCK`), `items[]`

6. **OrderCompleted** (emitido por `order-service`)  
   Topic: `orders.order-completed.v1`  
   Key: `orderId`

7. **OrderFailed** (emitido por `order-service`)  
   Topic: `orders.order-failed.v1`  
   Key: `orderId`
   Payload:
   - `orderId`, `failedAt`, `reasonStage` (`PAYMENT`/`INVENTORY`), `reasonCode`

8. **NotificationSent** (opcional de auditoría)  
   Topic: `notifications.notification-sent.v1`

---

## 5) Secuencia end-to-end del flujo

1. Cliente llama `POST /orders` en `order-service` con `Idempotency-Key` opcional.
2. `order-service` crea orden en estado `PENDING_PAYMENT` y escribe outbox `OrderCreated`.
3. Relay outbox publica `OrderCreated` a Kafka.
4. `payment-service` consume `OrderCreated`, deduplica, procesa pago.
5. `payment-service` publica `PaymentApproved` o `PaymentRejected`.
6. `order-service` consume resultado de pago:
   - si rechazo -> `FAILED` + publica `OrderFailed`.
   - si aprobado -> estado intermedio `PAYMENT_APPROVED`.
7. `inventory-service` consume `PaymentApproved`, intenta reserva.
8. `inventory-service` publica `InventoryReserved` o `InventoryFailed`.
9. `order-service` consume evento de inventario:
   - `InventoryReserved` -> `COMPLETED` + publica `OrderCompleted`.
   - `InventoryFailed` -> `FAILED` + publica `OrderFailed`.
10. `notification-service` consume `OrderCompleted/OrderFailed` y registra envío.
11. En caso de error técnico de consumo: retry y, al agotar, envío a DLT.

---

## 6) Estrategia de particionado, consumer groups y DLQ

## 6.1 Particionado
- Clave principal: **`orderId`** en todos los eventos del flujo principal.
- Justificación:
  - mantiene orden por agregado de negocio (orden)
  - evita race conditions entre estados de la misma orden
- Cantidad inicial sugerida: `6` particiones por topic principal (escalable).

## 6.2 Consumer groups
- `payment-service` group: `payment-service-v1`
- `inventory-service` group: `inventory-service-v1`
- `order-service` puede tener múltiples listeners con groups separados:
  - `order-service-payment-results-v1`
  - `order-service-inventory-results-v1`
- `notification-service` group: `notification-service-v1`

> Regla: mismo servicio escalado horizontalmente comparte mismo group; diferentes responsabilidades, groups distintos.

## 6.3 Retry y DLQ
- Usar `DefaultErrorHandler` de Spring Kafka + `DeadLetterPublishingRecoverer`.
- Backoff recomendado inicial:
  - 1s, 5s, 15s (3 intentos)
- Al agotar retries -> publicar a `*.dlt`.

Ejemplos:
- `payments.payment-approved.v1.dlt`
- `inventory.inventory-reserved.v1.dlt`

### 6.4 Clasificación de errores
- **No reintentar**: payload inválido / schema mismatch / validación de negocio no recuperable.
- **Reintentar**: timeouts, conexiones temporales, dependencia transitoria.

### 6.5 Reprocesamiento DLQ
- Job/manual consumer de replay con controles:
  - límite de throughput
  - trazabilidad de reintentos
  - no loop infinito DLT→origen→DLT

---

## 7) Decisiones técnicas justificadas

1. **Spring Boot + Spring Kafka**
   - Curva de aprendizaje adecuada y ecosistema sólido.
   - Integración natural con Actuator/Micrometer.

2. **PostgreSQL por servicio**
   - Aislamiento de datos por bounded context.
   - Permite practicar autonomía de servicios.

3. **Transactional Outbox**
   - Evita inconsistencias del dual write DB+Kafka.
   - Patrón clave en sistemas event-driven reales.

4. **JSON Schema/Avro versionado en `shared/event-contracts`**
   - Contratos explícitos, trazables y evolucionables.

5. **OpenTelemetry + Micrometer + Prometheus/Grafana**
   - Correlación de request REST con eventos Kafka.
   - Observabilidad didáctica y productiva.

6. **Hexagonal “light”**
   - Mantiene dominio limpio sin complejidad excesiva.

7. **Docker Compose integral local**
   - Reproducibilidad inmediata para onboarding.

8. **Kubernetes manifests base + overlays**
   - Camino de madurez sin exigir cloud desde día 1.

---

## 8) Roadmap de implementación por fases

### Fase 0 — Bootstrap del monorepo
- Gradle multi-módulo, convenciones de build y checkstyle/spotless.
- Docker Compose con Kafka, UI opcional, Postgres por servicio.
- Plantilla base Spring Boot por servicio.

### Fase 1 — Flujo mínimo feliz
- `order-service` crea orden y publica `OrderCreated`.
- `payment-service` consume y publica aprobación.
- `order-service` marca `COMPLETED` simplificado.
- Tests de integración con Testcontainers (Kafka + Postgres).

### Fase 2 — Flujo completo de negocio
- Incorporar `inventory-service` y eventos de falla.
- Estado final completo (`COMPLETED/FAILED`).
- `notification-service` consumidor final.

### Fase 3 — Resiliencia real
- Retries con backoff + DLT por consumer.
- Idempotencia en consumidores (tabla `processed_events`).
- Idempotency-Key en API de órdenes.

### Fase 4 — Observabilidad
- Actuator, Prometheus metrics, Grafana dashboards.
- Logs JSON con `correlationId`, `traceId`, `eventId`, `orderId`.
- Trazas distribuidas (OTel collector opcional).

### Fase 5 — Hardening + documentación
- ADRs y runbooks.
- Pruebas de contrato de eventos.
- Pruebas de carga básica (k6/Gatling) y tuning inicial Kafka consumers.

### Fase 6 — Kubernetes deployment
- Manifiestos base + overlays.
- ConfigMaps/Secrets, readiness/liveness probes, HPA básico.
- Guía de despliegue local en kind/k3d.

---

## 9) Riesgos y errores comunes

1. **Acoplar servicios con REST interno**
   - Rompe autonomía y resiliencia del diseño EDA.

2. **No versionar eventos**
   - Dificulta evolución y compatibilidad hacia atrás.

3. **No usar outbox**
   - Riesgo alto de inconsistencias por dual write.

4. **Ignorar idempotencia**
   - Kafka puede redeliver; sin dedupe habrá efectos duplicados.

5. **No definir keys de partición correctamente**
   - Se pierde orden lógico por agregado.

6. **Mezclar DTO de API con modelo de persistencia/eventos**
   - Acoplamiento accidental y deuda de mantenimiento.

7. **No monitorear lag y errores de consumo**
   - Problemas silenciosos hasta impactar negocio.

8. **DLQ sin runbook de reproceso**
   - Se transforma en “cementerio” de eventos.

9. **Sobreingeniería temprana**
   - Complica aprendizaje; mejor iterar por fases.

---

## 10) Criterios de “listo para aprender en serio”
- Se puede levantar todo con `docker compose up`.
- Flujo completo visible en UI de logs/metrics.
- Hay casos felices y fallidos reproducibles.
- Existen pruebas automatizadas de integración y contratos.
- Hay guías claras para operar, diagnosticar y reprocesar.

---

## 11) Próximo paso sugerido (sin implementar todo aún)
Con este blueprint aprobado, el siguiente paso es crear:
1. esqueleto monorepo + compose,
2. contratos de eventos v1,
3. `order-service` + `payment-service` con outbox,
4. test e2e mínimo con Testcontainers.

Eso entrega valor rápido y deja base firme para sumar `inventory`, DLQ y observabilidad avanzada.
