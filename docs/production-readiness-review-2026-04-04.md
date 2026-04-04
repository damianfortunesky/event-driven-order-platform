# Revisión crítica pre-producción (arquitectura senior)

Fecha: 2026-04-04

## Alcance
Evaluación integral del repo `event-driven-order-platform` enfocada en: acoplamiento, diseño de eventos, consistencia eventual, duplicación/idempotencia, fallos, observabilidad, seguridad, versionado, escalabilidad, riesgos operativos y errores típicos Kafka/EDA.

---

## 1) Hallazgos priorizados

### P0 (bloqueantes para producción)

1. **Contrato de eventos inconsistente entre servicios (rompe el flujo principal).**
   - `order-service` consume tópicos `payments.payment-processed.v1` e `inventory.inventory-processed.v1`, pero `payment-service` publica `payments.payment-approved.v1 | payments.payment-rejected.v1` e `inventory-service` publica `inventory.inventory-reserved.v1 | inventory.inventory-failed.v1`.
   - Además, `order-service` espera `eventType=PaymentProcessed|InventoryProcessed`, mientras que el resto publica `PaymentApproved|PaymentRejected|InventoryReserved|InventoryFailed`.
   - Riesgo: órdenes que nunca avanzan a estado final, acumulación de mensajes, divergencia funcional.

2. **Ausencia de publicación transaccional (Outbox) en los productores críticos.**
   - `order-service`, `payment-service` e `inventory-service` persisten estado y publican a Kafka sin patrón outbox ni confirmación transaccional cross-resource.
   - Riesgo: “DB commit sin evento” o “evento sin commit” ante caídas intermedias.

3. **Tratamiento de errores Kafka incompleto y heterogéneo.**
   - `payment-service` e `inventory-service` sí tienen `DefaultErrorHandler + backoff + DLQ`; `order-service` y `notification-service` no declaran estrategia equivalente en listeners.
   - Riesgo: reintentos no controlados, poison pills, offsets en estados no deseados y falta de DLQ consistente.

4. **Modelo de contrato duplicado y no alineado (docs vs schemas vs código).**
   - El catálogo de contratos define campos (`schemaVersion`, `producer`, `partitionKey`), pero el envelope implementado en código usa variante reducida y el schema v1 define `eventVersion/source`.
   - Riesgo: incompatibilidad entre equipos/consumidores, validaciones fallidas, migraciones costosas.

### P1 (alto impacto)

5. **Acoplamiento semántico fuerte por strings de estado/evento.**
   - Múltiples decisiones de negocio dependen de literales de texto (`APPROVED`, `RESERVED`, etc.) sin contrato tipado compartido.
   - Riesgo: errores por typo, cambios no coordinados, fragilidad ante evolución.

6. **Idempotencia parcial, no uniforme y con potenciales carreras.**
   - Hay mecanismos buenos (tablas `processed_events`, `inventory_inbox_event`, unique constraints), pero no estándar único entre servicios.
   - Riesgo: duplicados lógicos, comportamiento distinto por servicio bajo redelivery.

7. **Observabilidad útil pero insuficiente para operación de incidentes complejos.**
   - Hay métricas base y `correlationId`, pero faltan: tracing distribuido (W3C/OpenTelemetry), métricas de lag por consumer group, tasa/edad de DLQ por tópico, SLO/SLA por flujo de negocio.

8. **Seguridad orientada a local-dev, no hardening productivo.**
   - Credenciales por defecto en configs/.env, sin evidencia de TLS/SASL Kafka activo ni autenticación/autorización en APIs internas.
   - Riesgo: exposición de secretos y tráfico no cifrado en entornos mal configurados.

### P2 (medio)

9. **Escalabilidad no validada para crecimiento real.**
   - Falta estrategia explícita de particionado por dominio caliente, límites de concurrencia por consumidor, pruebas de carga/chaos y capacity planning.

10. **Servicio de shipping incompleto respecto al diseño de referencia.**
   - Hay skeleton/configuración pero no se observa flujo de consumo/publicación operativo.

11. **Deuda de gobernanza de contratos.**
   - Solo se ve un schema de payload (`order-created.v1.json`) y no hay pipeline visible de compatibilidad automática para todos los eventos.

---

## 2) Mejoras recomendadas

1. **Normalizar el “backbone” de eventos (primera prioridad).**
   - Definir un set canónico de eventos finales y mapear 1:1 tópicos + `eventType` + payload.
   - Corregir `order-service` para consumir los eventos realmente emitidos por payment/inventory.

2. **Adoptar Outbox + relay (CDC o poller) en productores de dominio.**
   - Persistir cambios de negocio y outbox en una misma transacción local.
   - Publicador asíncrono reintentable con marcas de envío y replay seguro.

3. **Estandarizar manejo de errores Kafka en todos los servicios.**
   - `DefaultErrorHandler`, retries exponenciales, clasificación retryable/no-retryable, DLQ por tópico y métricas homogéneas.
   - Definir política corporativa de commits/acks y timeouts.

4. **Gobernanza de contratos estricta.**
   - Unificar envelope (campos obligatorios, nombres y tipos).
   - Schema Registry + compatibility checks en CI (backward/forward según estrategia).

5. **Idempotencia “by default” en toda frontera.**
   - Regla estándar: `idempotency_key = eventId` + unique constraint + short-circuit.
   - Donde aplique, deterministic eventId derivado de causa de negocio.

6. **Observabilidad de nivel producción.**
   - OpenTelemetry (trace/span), propagación automática de contexto, dashboards por saga.
   - Alertas: lag alto, DLQ crecimiento, retries anómalos, latencia end-to-end de order lifecycle.

7. **Seguridad productiva.**
   - TLS/SASL en Kafka, rotación de secretos, secret manager, mínimos privilegios DB/Kafka ACL.
   - Revisar exposición de endpoints Actuator y endurecer network policies.

---

## 3) Quick wins (1–2 semanas)

1. Corregir topic bindings y `eventType` en `order-service` para alinear con eventos existentes.
2. Añadir `KafkaConfig` con `DefaultErrorHandler + DLQ` en `order-service` y `notification-service`.
3. Publicar catálogo único “source of truth” (evitar duplicidad `docs/event-catalog.md` vs `contracts/events/topic-catalog.md`).
4. Agregar tests de contrato consumidor/productor para `PaymentApproved`, `PaymentRejected`, `InventoryReserved`, `InventoryFailed`.
5. Crear dashboard operativo mínimo: lag por consumer group + DLQ por tópico + throughput + errores por tipo.
6. Eliminar defaults inseguros en configs fuera de entorno local y documentar baseline de hardening.

---

## 4) Deuda técnica

- **Arquitectura de mensajería:** falta outbox y política uniforme de errores/reintentos/DLQ.
- **Dominio y contratos:** nombres/semántica de eventos no totalmente convergentes.
- **Governance:** cobertura incompleta de schemas y validaciones de compatibilidad automáticas.
- **Operación:** runbooks buenos pero falta instrumentación avanzada para MTTR bajo.
- **Seguridad:** defaults de desarrollo y controles de producción no reforzados en código/config.

---

## 5) Plan de evolución a versión más productiva

### Fase 0 (0–2 semanas) — Estabilización funcional
- Alinear tópicos/eventTypes end-to-end.
- Homogeneizar error handling Kafka + DLQ en todos los consumidores.
- Tests de integración cruzada order/payment/inventory/notification en pipeline.

### Fase 1 (2–6 semanas) — Confiabilidad
- Introducir outbox en order/payment/inventory.
- Definir contrato envelope v2 (o v1 consolidado) + migración controlada.
- Implementar compatibility checks automáticos y consumer-driven contracts.

### Fase 2 (6–10 semanas) — Operabilidad y seguridad
- Incorporar tracing distribuido (OpenTelemetry) y SLOs del flujo de orden.
- Alertas accionables (lag, DLQ, retries, latencia de saga).
- Hardening: TLS/SASL Kafka, ACLs, secretos centralizados, rotación.

### Fase 3 (10+ semanas) — Escala y excelencia operativa
- Pruebas de carga/caos (broker restart, network partitions, DB failover).
- Estrategia de particiones y throughput por dominio caliente.
- Automatizar reprocess de DLQ con guardrails y auditoría.

---

## Errores frecuentes de Kafka (y cómo mitigarlos aquí)

1. **Consumer lag oculto**: medir lag por grupo y alertar por umbral/pendiente.
2. **Poison pills**: clasificar errores no reintentables y desviar a DLQ rápido.
3. **Rebalances frecuentes**: ajustar session/poll intervals, evitar handlers lentos sin backpressure.
4. **Particionado inconsistente**: fijar key estable por `orderId` en todos los eventos del flujo.
5. **Semántica exactly-once mal entendida**: combinar idempotencia de aplicación + outbox; EOS sola no resuelve side effects externos.

## Errores frecuentes de EDA (y cómo mitigarlos aquí)

1. **Eventos “hechos a medida” por servicio**: centralizar contrato canónico y gobernanza.
2. **Acoplamiento por conocimiento implícito**: explicitar estados/eventos con contrato formal y versionado.
3. **No diseñar para duplicados**: forzar idempotencia en consumidores y operaciones de escritura.
4. **Falta de observabilidad de negocio**: medir latencia de saga, tasa de cancelación por causa, stuck orders.
5. **Reprocesos manuales riesgosos**: playbooks + tooling de replay con filtros, dry-run y auditoría.
