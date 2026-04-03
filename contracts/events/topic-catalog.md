# Catálogo de Eventos Kafka — Order Platform

> Estado: **Propuesto para implementación**  
> Versión del catálogo: **1.0.0**  
> Última actualización: **2026-04-03**

## 1) Convenciones globales

### 1.1 Envelope estándar (obligatorio para todos los eventos)
Todos los mensajes deben publicarse con un envelope común:

```json
{
  "eventId": "uuid-v4",
  "eventType": "OrderCreated",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:30Z",
  "producer": "order-service",
  "correlationId": "uuid-v4",
  "partitionKey": "ord_12345",
  "payload": {}
}
```

Campos obligatorios del envelope:
- `eventId` (UUID, único por evento)
- `eventType` (nombre canónico del evento)
- `schemaVersion` (entero >= 1)
- `occurredAt` (ISO-8601 UTC)
- `producer` (servicio emisor)
- `correlationId` (UUID de la saga/flujo)
- `partitionKey` (clave de ordenado en Kafka)
- `payload` (objeto, schema versionado)

> Nota: el repositorio ya define `eventVersion` en el schema envelope v1. Para estandarizar con este catálogo, se recomienda migrar a `schemaVersion` o soportar ambos temporalmente (`schemaVersion` preferido, `eventVersion` deprecado).

### 1.2 Convención de nombres de topics
Se define la convención:

`<dominio>.<tipo-evento-kebab-case>.v<version>`

Ejemplos:
- `orders.order-created.v1`
- `payments.payment-requested.v1`
- `inventory.inventory-reserved.v1`

Reglas:
- `dominio`: `orders`, `payments`, `inventory`, `notifications`.
- `version`: versión mayor del contrato del evento (`v1`, `v2`, ...).
- No reutilizar un topic versionado para cambios incompatibles.

### 1.3 Convención de keys de partición
- Key por defecto para esta plataforma: **`orderId`**.
- Objetivo: preservar orden por orden de negocio en todos los eventos de la saga.
- Formato recomendado: string estable (`ord_...` o UUID de orden).

---

## 2) Catálogo mínimo de eventos

## 2.1 `OrderCreated`
- **nombre**: `OrderCreated`
- **versión**: `1`
- **topic**: `orders.order-created.v1`
- **key de partición**: `payload.orderId`
- **productor**: `order-service`
- **consumidores**: `payment-service`
- **tipo**: Domain Event
- **cuándo se emite**: al persistir una orden en estado `PENDING_PAYMENT`.
- **campos obligatorios**:
  - Envelope: `eventId`, `eventType`, `schemaVersion`, `occurredAt`, `producer`, `correlationId`, `partitionKey`, `payload`
  - Payload: `orderId`, `customerId`, `currency`, `totalAmount`, `items`, `createdAt`

**Ejemplo JSON completo**
```json
{
  "eventId": "8c7ae6b8-742d-4d03-9ef0-18f0b0cde1e2",
  "eventType": "OrderCreated",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:30Z",
  "producer": "order-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "customerId": "cus_8891",
    "currency": "USD",
    "totalAmount": 129.98,
    "items": [
      { "sku": "SKU-BOOK-001", "quantity": 1, "unitPrice": 79.99 },
      { "sku": "SKU-PEN-002", "quantity": 2, "unitPrice": 24.995 }
    ],
    "createdAt": "2026-04-03T10:15:28Z"
  }
}
```

## 2.2 `PaymentRequested`
- **nombre**: `PaymentRequested`
- **versión**: `1`
- **topic**: `payments.payment-requested.v1`
- **key de partición**: `payload.orderId`
- **productor**: `order-service`
- **consumidores**: `payment-service`
- **tipo**: Command-like Event
- **cuándo se emite**: cuando la orden requiere inicio explícito del cobro.
- **campos obligatorios (payload)**: `orderId`, `paymentId`, `customerId`, `amount`, `currency`, `requestedAt`

**Ejemplo JSON completo**
```json
{
  "eventId": "3058f6ab-bd20-4f0f-a60c-6337f8d4722d",
  "eventType": "PaymentRequested",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:32Z",
  "producer": "order-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "paymentId": "pay_70001",
    "customerId": "cus_8891",
    "amount": 129.98,
    "currency": "USD",
    "paymentMethodRef": "pm_tok_xxx",
    "requestedAt": "2026-04-03T10:15:32Z"
  }
}
```

## 2.3 `PaymentApproved`
- **nombre**: `PaymentApproved`
- **versión**: `1`
- **topic**: `payments.payment-approved.v1`
- **key de partición**: `payload.orderId`
- **productor**: `payment-service`
- **consumidores**: `order-service`, `inventory-service`
- **tipo**: Domain Event
- **cuándo se emite**: al confirmar autorización/captura de pago.
- **campos obligatorios (payload)**: `orderId`, `paymentId`, `approvedAt`, `amount`, `currency`

**Ejemplo JSON completo**
```json
{
  "eventId": "6b4ce2bb-937f-486f-a355-a8f7a97bbf45",
  "eventType": "PaymentApproved",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:40Z",
  "producer": "payment-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "paymentId": "pay_70001",
    "amount": 129.98,
    "currency": "USD",
    "providerTransactionId": "ch_1ABCD",
    "approvedAt": "2026-04-03T10:15:39Z"
  }
}
```

## 2.4 `PaymentRejected`
- **nombre**: `PaymentRejected`
- **versión**: `1`
- **topic**: `payments.payment-rejected.v1`
- **key de partición**: `payload.orderId`
- **productor**: `payment-service`
- **consumidores**: `order-service`
- **tipo**: Domain Event
- **cuándo se emite**: cuando el pago es rechazado de forma final.
- **campos obligatorios (payload)**: `orderId`, `paymentId`, `rejectedAt`, `reasonCode`

**Ejemplo JSON completo**
```json
{
  "eventId": "5f7048a0-c89a-42ef-b812-c58f1d7cbbe8",
  "eventType": "PaymentRejected",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:41Z",
  "producer": "payment-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "paymentId": "pay_70001",
    "reasonCode": "INSUFFICIENT_FUNDS",
    "reasonMessage": "Card declined by issuer",
    "rejectedAt": "2026-04-03T10:15:41Z"
  }
}
```

## 2.5 `InventoryReservationRequested`
- **nombre**: `InventoryReservationRequested`
- **versión**: `1`
- **topic**: `inventory.inventory-reservation-requested.v1`
- **key de partición**: `payload.orderId`
- **productor**: `order-service`
- **consumidores**: `inventory-service`
- **tipo**: Command-like Event
- **cuándo se emite**: después de `PaymentApproved`, para solicitar reserva de stock.
- **campos obligatorios (payload)**: `orderId`, `reservationId`, `items`, `requestedAt`

**Ejemplo JSON completo**
```json
{
  "eventId": "a659abf8-f577-4f71-bcc1-55de52f00a2e",
  "eventType": "InventoryReservationRequested",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:43Z",
  "producer": "order-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "reservationId": "res_30001",
    "items": [
      { "sku": "SKU-BOOK-001", "quantity": 1 },
      { "sku": "SKU-PEN-002", "quantity": 2 }
    ],
    "requestedAt": "2026-04-03T10:15:43Z"
  }
}
```

## 2.6 `InventoryReserved`
- **nombre**: `InventoryReserved`
- **versión**: `1`
- **topic**: `inventory.inventory-reserved.v1`
- **key de partición**: `payload.orderId`
- **productor**: `inventory-service`
- **consumidores**: `order-service`, `shipping-service`
- **tipo**: Domain Event
- **cuándo se emite**: al confirmar reserva completa de stock.
- **campos obligatorios (payload)**: `orderId`, `reservationId`, `reservedAt`, `items`

**Ejemplo JSON completo**
```json
{
  "eventId": "2dd34754-f8ce-43d4-83f5-af2cd58d8d65",
  "eventType": "InventoryReserved",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:49Z",
  "producer": "inventory-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "reservationId": "res_30001",
    "items": [
      { "sku": "SKU-BOOK-001", "quantity": 1 },
      { "sku": "SKU-PEN-002", "quantity": 2 }
    ],
    "reservedAt": "2026-04-03T10:15:48Z"
  }
}
```

## 2.7 `InventoryFailed`
- **nombre**: `InventoryFailed`
- **versión**: `1`
- **topic**: `inventory.inventory-failed.v1`
- **key de partición**: `payload.orderId`
- **productor**: `inventory-service`
- **consumidores**: `order-service`
- **tipo**: Domain Event
- **cuándo se emite**: cuando no se puede reservar stock total.
- **campos obligatorios (payload)**: `orderId`, `reservationId`, `failedAt`, `reasonCode`

**Ejemplo JSON completo**
```json
{
  "eventId": "909cecc3-1a76-4f43-9a6e-82af0f937ca0",
  "eventType": "InventoryFailed",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:50Z",
  "producer": "inventory-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "reservationId": "res_30001",
    "reasonCode": "OUT_OF_STOCK",
    "reasonMessage": "SKU-PEN-002 unavailable",
    "failedAt": "2026-04-03T10:15:50Z"
  }
}
```

## 2.8 `OrderConfirmed`
- **nombre**: `OrderConfirmed`
- **versión**: `1`
- **topic**: `orders.order-confirmed.v1`
- **key de partición**: `payload.orderId`
- **productor**: `order-service`
- **consumidores**: `notification-service`, `shipping-service`
- **tipo**: Domain Event
- **cuándo se emite**: al completar exitosamente pago + inventario.
- **campos obligatorios (payload)**: `orderId`, `confirmedAt`, `customerId`

**Ejemplo JSON completo**
```json
{
  "eventId": "8b8995c8-358c-4ca6-8415-988f09d8497f",
  "eventType": "OrderConfirmed",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:52Z",
  "producer": "order-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "customerId": "cus_8891",
    "confirmedAt": "2026-04-03T10:15:52Z"
  }
}
```

## 2.9 `OrderCancelled`
- **nombre**: `OrderCancelled`
- **versión**: `1`
- **topic**: `orders.order-cancelled.v1`
- **key de partición**: `payload.orderId`
- **productor**: `order-service`
- **consumidores**: `notification-service`, `payment-service`, `inventory-service`
- **tipo**: Domain Event
- **cuándo se emite**: ante fallo definitivo de pago/inventario o cancelación de negocio.
- **campos obligatorios (payload)**: `orderId`, `cancelledAt`, `reasonCode`

**Ejemplo JSON completo**
```json
{
  "eventId": "c4d945e6-a0f0-4b55-98e5-5e77b47d0ad1",
  "eventType": "OrderCancelled",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:52Z",
  "producer": "order-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "cancelledAt": "2026-04-03T10:15:52Z",
    "reasonCode": "PAYMENT_REJECTED",
    "reasonMessage": "Payment authorization failed"
  }
}
```

## 2.10 `NotificationRequested`
- **nombre**: `NotificationRequested`
- **versión**: `1`
- **topic**: `notifications.notification-requested.v1`
- **key de partición**: `payload.orderId`
- **productor**: `order-service`
- **consumidores**: `notification-service`
- **tipo**: Command-like Event
- **cuándo se emite**: al requerir envío explícito de comunicación al cliente.
- **campos obligatorios (payload)**: `orderId`, `notificationId`, `channel`, `templateCode`, `requestedAt`

**Ejemplo JSON completo**
```json
{
  "eventId": "a00dcb5d-eb74-4989-b90b-f2cb17f7b815",
  "eventType": "NotificationRequested",
  "schemaVersion": 1,
  "occurredAt": "2026-04-03T10:15:55Z",
  "producer": "order-service",
  "correlationId": "a59ec9f6-65fe-4e0b-b9e4-58f08bf9d4e8",
  "partitionKey": "ord_10001",
  "payload": {
    "orderId": "ord_10001",
    "notificationId": "not_50001",
    "channel": "EMAIL",
    "templateCode": "ORDER_CONFIRMED_V1",
    "requestedAt": "2026-04-03T10:15:55Z",
    "recipient": {
      "customerId": "cus_8891",
      "email": "user@example.com"
    },
    "variables": {
      "orderId": "ord_10001"
    }
  }
}
```

---

## 3) Clasificación: command-like vs domain events

### Command-like (intención dirigida a un consumidor principal)
- `PaymentRequested`
- `InventoryReservationRequested`
- `NotificationRequested`

Características:
- Representan una **solicitud de acción**.
- Deben tener consumidor principal bien definido.
- Recomendado incluir timeout/reintento idempotente.

### Domain Events (hechos ya ocurridos)
- `OrderCreated`
- `PaymentApproved`
- `PaymentRejected`
- `InventoryReserved`
- `InventoryFailed`
- `OrderConfirmed`
- `OrderCancelled`

Características:
- Describen hechos inmutables del dominio.
- Pueden tener múltiples consumidores.
- Evitar semántica imperativa en el nombre.

---

## 4) Estrategia de DLQ recomendada

## 4.1 Decisión
Adoptar estrategia **DLQ por topic de consumo** (no una única DLQ por servicio).

Formato recomendado:
- Topic principal: `<dominio>.<evento>.vN`
- Retry topic(s): `<dominio>.<evento>.vN.retry.<intento>`
- DLQ final: `<dominio>.<evento>.vN.dlq`

Ejemplo:
- `payments.payment-requested.v1`
- `payments.payment-requested.v1.retry.1`
- `payments.payment-requested.v1.dlq`

## 4.2 Justificación
- Aísla fallos por contrato/evento.
- Facilita observabilidad, ownership y reproceso selectivo.
- Evita mezclar errores heterogéneos en una sola DLQ masiva.

## 4.3 Headers mínimos para DLQ
- `x-original-topic`
- `x-error-class`
- `x-error-message`
- `x-retry-count`
- `x-failed-at`
- `x-correlation-id`
- `x-event-id`

---

## 5) Reglas de compatibilidad futura

1. **Compatible hacia atrás (misma major):**
   - Permitir agregar campos opcionales en `payload`.
   - No eliminar ni cambiar tipo de campos obligatorios.
   - No cambiar semántica de campos existentes.

2. **Cambios incompatibles:**
   - Requieren nueva versión mayor (`v2`) y nuevo topic.
   - Mantener coexistencia temporal de `v1` y `v2`.

3. **Estrategia de deprecación:**
   - Marcar campos/eventos como deprecados en catálogo.
   - Definir fecha de sunset por versión.

4. **Idempotencia obligatoria en consumidores:**
   - Dedupe por `eventId` + `eventType`.
   - Procesamiento tolerante a redelivery.

5. **Trazabilidad:**
   - `correlationId` constante durante toda la saga de orden.
   - `eventId` único por publicación.

---

## 6) Guía para evitar payloads acoplados

- Publicar solo datos de integración necesarios (no modelo interno completo).
- No exponer estructuras persistentes internas (tablas/IDs técnicos ajenos).
- Usar `payload` orientado a caso de uso, no a entidad anémica gigante.
- Referenciar recursos por ID y resolver detalles por API read-model cuando sea necesario.
- Mantener campos estables y semánticos (`reasonCode`, `status`, `amount`).
- Evitar anidar objetos profundos salvo datos estrictamente requeridos.
- Para datos altamente variables, encapsular en sub-objeto explícito (ej. `metadata`) y documentar contrato.

---

## 7) Matriz resumida (operativa)

| Evento | Topic | Tipo | Productor | Consumidor(es) | Partition Key |
|---|---|---|---|---|---|
| OrderCreated | `orders.order-created.v1` | Domain | order-service | payment-service | `orderId` |
| PaymentRequested | `payments.payment-requested.v1` | Command-like | order-service | payment-service | `orderId` |
| PaymentApproved | `payments.payment-approved.v1` | Domain | payment-service | order-service, inventory-service | `orderId` |
| PaymentRejected | `payments.payment-rejected.v1` | Domain | payment-service | order-service | `orderId` |
| InventoryReservationRequested | `inventory.inventory-reservation-requested.v1` | Command-like | order-service | inventory-service | `orderId` |
| InventoryReserved | `inventory.inventory-reserved.v1` | Domain | inventory-service | order-service, shipping-service | `orderId` |
| InventoryFailed | `inventory.inventory-failed.v1` | Domain | inventory-service | order-service | `orderId` |
| OrderConfirmed | `orders.order-confirmed.v1` | Domain | order-service | notification-service, shipping-service | `orderId` |
| OrderCancelled | `orders.order-cancelled.v1` | Domain | order-service | notification-service, payment-service, inventory-service | `orderId` |
| NotificationRequested | `notifications.notification-requested.v1` | Command-like | order-service | notification-service | `orderId` |
