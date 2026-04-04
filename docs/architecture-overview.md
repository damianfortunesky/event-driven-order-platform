# Architecture Overview

## 1. Objetivo

Procesar órdenes de forma asíncrona y resiliente, desacoplando dominios (Orders, Payments, Inventory, Shipping, Notifications) mediante eventos Kafka versionados.

## 2. Principios de diseño

- **Domain ownership**: cada servicio es dueño de su modelo y base de datos.
- **Asynchronous first**: integración principal por eventos, no por llamadas síncronas acopladas.
- **Contract-first**: contratos JSON versionados en `contracts/`.
- **Idempotency by default**: deduplicación en consumidores para tolerar redelivery.
- **Operational resilience**: retry exponencial + DLQ + runbooks.
- **Observability**: métricas, logs estructurados y correlación de eventos.

## 3. Componentes principales

- **order-service**: crea/actualiza órdenes y publica eventos de ciclo de vida.
- **payment-service**: decide aprobación/rechazo de pago.
- **inventory-service**: reserva stock y publica resultado.
- **shipping-service**: etapa logística post-reserva.
- **notification-service**: emite notificaciones al cliente.
- **Kafka**: backbone de integración asíncrona.
- **PostgreSQL por servicio**: persistencia aislada por bounded context.

## 4. Flujo funcional resumido

1. `order-service` publica `OrderCreated`.
2. `payment-service` consume y publica `PaymentApproved` o `PaymentRejected`.
3. Si hay aprobación, `inventory-service` publica `InventoryReserved` o `InventoryFailed`.
4. `order-service` consume resultados y decide `OrderConfirmed` u `OrderCancelled`.
5. `notification-service` consume eventos finales y registra notificaciones.

## 5. Decisiones técnicas clave

- Naming convention de tópicos con versión (`*.v1`) para evolución segura.
- Estrategia de retries con backoff exponencial y fallback a DLQ.
- Uso recomendado de outbox pattern para publicación transaccional.
- Uso de `correlationId` en payload/logs para trazabilidad distribuida.

## 6. Escalabilidad

- Escalado horizontal por servicio con consumer groups.
- Orden por agregado garantizado usando `orderId` como key.
- Ajuste de particiones por throughput del dominio.

## 7. Seguridad (lineamientos)

- Secretos fuera del repo (`.env` local, Secret manager en entornos remotos).
- Cifrado en tránsito (TLS) en Kafka y conexiones de base en ambientes productivos.
- Control de acceso por principio de mínimo privilegio.
