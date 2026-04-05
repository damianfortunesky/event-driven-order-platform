# Playbook de implementación guiada (modo mentor)

Este playbook define **cómo vamos a implementar este proyecto mientras aprendés**. Está orientado al repositorio real (`order-service`, `payment-service`, `inventory-service`, `notification-service`) y evita teoría abstracta.

---

## Cómo vamos a trabajar en cada etapa

En cada etapa voy a seguir esta secuencia:

1. **Problema real que resolvemos en la plataforma** (no teoría genérica).
2. **Diseño propuesto** con trade-offs explícitos.
3. **Implementación** (clases/config/eventos concretos).
4. **Validación** (tests + verificación operativa).
5. Cierre con estas 3 secciones obligatorias:
   - **Qué aprendiste de Kafka/EDA en esta etapa**
   - **Errores comunes**
   - **Siguiente paso recomendado**

Además:
- Antes de crear código, se explica para qué sirve cada parte.
- Al crear una clase, se explica por qué esa clase existe y cuál es su límite de responsabilidad.
- Al tocar Kafka, se explica cada propiedad importante (producer, consumer, retries, ack, idempotencia, etc.).
- Al diseñar eventos, se justifica el payload (qué campos entran, qué campos quedan fuera y por qué).
- Si agregamos retries o DLQ, se justifica el motivo arquitectónico (resiliencia, aislamiento de fallos, re-procesamiento seguro).

---

## Etapa 1 — Pedido y publicación de `OrderCreated`

### Problema que resuelve
Necesitamos iniciar el flujo distribuido sin acoplar `order-service` a `payment-service`. Si `order-service` llamara por HTTP síncrono a todos los pasos, tendríamos una cascada frágil y difícil de escalar.

### Diseño en este proyecto
- `order-service` persiste la orden y publica `OrderCreated` a Kafka.
- Partition key: `orderId` para mantener orden por agregado.
- Contrato estable en `contracts/schemas/order-created.v1.json`.

**Trade-offs**
- ✅ Desacople temporal y escalabilidad por consumidores.
- ⚠️ Mayor complejidad operativa (broker, observabilidad, reintentos).

### Clases típicas y por qué existen
- `OrderService` (caso de uso): orquesta reglas de aplicación.
- `KafkaOrderEventPublisher`: adapta dominio a infraestructura Kafka.
- DTO de evento (`OrderCreatedEventPayload`): protege el dominio de detalles de transporte.

### Qué aprendiste de Kafka/EDA en esta etapa
- Un evento de dominio representa un **hecho pasado** inmutable.
- La partition key correcta (`orderId`) evita desorden en transiciones de estado.
- Contratos versionados reducen rupturas entre equipos.

### Errores comunes
- Publicar eventos “anémicos” sin contexto mínimo (`orderId`, `customerId`, `items`, `timestamp`).
- Usar payloads acoplados a tablas internas.
- No validar esquema antes de consumir.

### Siguiente paso recomendado
Implementar consumo de `OrderCreated` en `payment-service` con idempotencia y manejo de errores transitorios.

---

## Etapa 2 — `payment-service`: consumo, decisión y evento de salida

### Problema que resuelve
Convertir un evento de entrada en una decisión de negocio (`PaymentApproved` / `PaymentRejected`) sin bloquear al productor.

### Diseño en este proyecto
- Consumer en `payment-service` procesa `OrderCreated`/`PaymentRequested`.
- Lógica de decisión encapsulada en `PaymentDecisionEngine`.
- Publicación de evento resultado para continuar la saga.

**Trade-offs**
- ✅ Aislamos reglas de pago del resto de dominios.
- ⚠️ Duplicados posibles por semántica at-least-once; se exige idempotencia.

### Config Kafka (propiedades clave a explicar siempre)
- `enable.auto.commit=false`: evitamos confirmar offsets antes de terminar lógica de negocio.
- `ack-mode`/commit manual: control fino del “procesado exitoso”.
- `max.poll.records`: balance entre throughput y latencia.
- `isolation.level=read_committed` (si usamos transacciones): evita leer mensajes abortados.

### Qué aprendiste de Kafka/EDA en esta etapa
- Consumir no es “leer y ya”: incluye control de offset y semántica de entrega.
- La idempotencia es requisito de arquitectura, no “mejora opcional”.

### Errores comunes
- Commit de offset antes de persistir efectos.
- Mezclar validación de contrato con reglas de negocio en una sola clase.
- No separar errores recuperables vs no recuperables.

### Siguiente paso recomendado
Agregar estrategia de retry + DLQ en consumidores críticos.

---

## Etapa 3 — Retries y DLQ en consumidores

### Problema que resuelve
Sin retries/DLQ, un mensaje “venenoso” puede bloquear consumo o perderse por descarte manual.

### Diseño en este proyecto
- Retries con backoff para fallos transitorios (red, lock, timeouts).
- DLQ para fallos no recuperables (payload inválido, violación de contrato).
- Runbook de reproceso controlado (ya documentado en `docs/runbooks/incident-dlq-reprocessing.md`).

**Trade-offs**
- ✅ Resiliencia y continuidad operativa.
- ⚠️ Más tópicos, más observabilidad y disciplina de operación.

### Motivo arquitectónico (por qué así y no todo retry)
- Reintentar errores no recuperables consume recursos y no mejora tasa de éxito.
- DLQ preserva evidencia para diagnóstico y reproceso seguro.

### Qué aprendiste de Kafka/EDA en esta etapa
- Retry y DLQ son parte del diseño funcional, no solo de infraestructura.
- Diseñar errores por categoría mejora SLO y MTTR.

### Errores comunes
- Backoff fijo demasiado agresivo.
- DLQ sin metadatos de causa (`errorType`, `stack`, `originalTopic`, `attempt`).
- No definir proceso de replay (DLQ sin runbook).

### Siguiente paso recomendado
Instrumentar métricas por tipo de error y alertas sobre crecimiento de DLQ.

---

## Etapa 4 — Inventario y eventos finales

### Problema que resuelve
Completar la saga con resultado final (`OrderConfirmed` o `OrderCancelled`) y notificación al cliente.

### Diseño en este proyecto
- `inventory-service` consume aprobación de pago y emite resultado de reserva.
- `order-service` consolida estado final.
- `notification-service` consume eventos finales y registra notificación.

**Trade-offs**
- ✅ Flujo extensible (sumar nuevos consumidores sin tocar productores).
- ⚠️ Mayor necesidad de trazabilidad (`correlationId`, observabilidad distribuida).

### Qué aprendiste de Kafka/EDA en esta etapa
- Un flujo EDA maduro se valida end-to-end, no servicio por servicio aislado.
- Los eventos finales permiten automatizar comunicación sin acoplar canales.

### Errores comunes
- Falta de consistencia en `correlationId` entre servicios.
- Modelar eventos “comando” en lugar de “hechos”.
- No testear reordenamiento o duplicación de mensajes.

### Siguiente paso recomendado
Agregar pruebas de caos y latencia para validar comportamiento bajo degradación.
