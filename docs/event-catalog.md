# Event Catalog

> Catálogo funcional de eventos de negocio. Para contrato técnico exacto usar `contracts/`.

## Convenciones

- Topic: `<bounded-context>.<event-name>.v<version>`.
- `eventType`: `PascalCase`.
- Clave Kafka: `orderId`.
- Metadata mínima: `eventId`, `occurredAt`, `correlationId`, `payload`.

## Eventos principales

### 1) OrderCreated
- **Topic**: `orders.order-created.v1`
- **Producer**: `order-service`
- **Consumers**: `payment-service`
- **Propósito**: iniciar evaluación de pago.

### 2) PaymentApproved
- **Topic**: `payments.payment-approved.v1`
- **Producer**: `payment-service`
- **Consumers**: `inventory-service`, `order-service`
- **Propósito**: habilitar reserva de inventario y transición de orden.

### 3) PaymentRejected
- **Topic**: `payments.payment-rejected.v1`
- **Producer**: `payment-service`
- **Consumers**: `order-service`, `notification-service`
- **Propósito**: cancelar orden por fallo de pago.

### 4) InventoryReserved
- **Topic**: `inventory.inventory-reserved.v1`
- **Producer**: `inventory-service`
- **Consumers**: `order-service`, `shipping-service`
- **Propósito**: confirmar que la orden puede continuar a logística.

### 5) InventoryFailed
- **Topic**: `inventory.inventory-failed.v1`
- **Producer**: `inventory-service`
- **Consumers**: `order-service`, `notification-service`
- **Propósito**: cancelar orden por falta de stock.

### 6) OrderConfirmed
- **Topic**: `orders.order-confirmed.v1`
- **Producer**: `order-service`
- **Consumers**: `notification-service`
- **Propósito**: cerrar flujo exitoso.

### 7) OrderCancelled
- **Topic**: `orders.order-cancelled.v1`
- **Producer**: `order-service`
- **Consumers**: `notification-service`
- **Propósito**: cerrar flujo fallido con motivo.

## Eventos técnicos

### DLQ (`<topic>.dlq`)
- Se usan para mensajes que agotaron retries o fueron clasificados como no-retryables.
- Operación sugerida: inspección, corrección y reprocesamiento controlado.

## Checklist para agregar un nuevo evento

1. Definir schema en `contracts/schemas`.
2. Versionar tópico y actualizar catálogo.
3. Agregar validación de compatibilidad de contrato.
4. Implementar idempotencia en consumidores.
5. Documentar retries/DLQ y runbook asociado.
