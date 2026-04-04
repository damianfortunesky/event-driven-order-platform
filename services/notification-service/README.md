# notification-service

Microservicio Spring Boot 3 + Java 21 para registrar notificaciones finales del flujo de órdenes.

## Responsabilidad
- Consumir eventos finales (`OrderConfirmed`, `OrderCancelled`, `PaymentRejected`, `InventoryFailed`).
- Generar notificaciones y persistirlas en PostgreSQL.
- Simular envío por canal email mediante logs estructurados.
- Exponer API opcional para consultar historial.

## Eventos Kafka consumidos
- `orders.order-confirmed.v1`
- `orders.order-cancelled.v1`
- `payments.payment-rejected.v1`
- `inventory.inventory-failed.v1`

## API (opcional)
- `GET /api/notifications`
- `GET /api/notifications?orderId=ord-123`

## Idempotencia
Se implementa deduplicación por par `(source_event_id, source_event_type)` con constraint única en base de datos.
Además, el servicio captura `DataIntegrityViolationException` para cubrir condiciones de carrera cuando el mismo evento llega en paralelo.

## Ejemplos de notificaciones generadas
1. **OrderConfirmed**
   - Subject: `Tu orden fue confirmada`
   - Body: `La orden ord_10001 fue confirmada correctamente.`
2. **OrderCancelled**
   - Subject: `Tu orden fue cancelada`
   - Body: `La orden ord_10001 fue cancelada. Motivo: PAYMENT_REJECTED.`
3. **PaymentRejected**
   - Subject: `Pago rechazado`
   - Body: `No se pudo procesar el pago de la orden ord_10001. Motivo: INSUFFICIENT_FUNDS.`
4. **InventoryFailed**
   - Subject: `Problema con inventario`
   - Body: `No pudimos reservar inventario para la orden ord_10001. Motivo: OUT_OF_STOCK.`

## Ejemplos de eventos de entrada (Kafka JSON)
```json
{
  "eventId": "f57f98c9-6ad8-40af-a272-7ad6d4d72af7",
  "eventType": "OrderCancelled",
  "occurredAt": "2026-04-03T10:15:30Z",
  "correlationId": "corr-001",
  "payload": {
    "orderId": "ord_10001",
    "customerId": "cus_123",
    "customerEmail": "cliente@example.com",
    "reasonCode": "PAYMENT_REJECTED"
  }
}
```

## Ejecutar local
```bash
cd services/notification-service
mvn spring-boot:run
```

## Variables de entorno clave
- `KAFKA_BOOTSTRAP_SERVERS`
- `NOTIFICATION_DB_URL`
- `NOTIFICATION_DB_USERNAME`
- `NOTIFICATION_DB_PASSWORD`
- `NOTIFICATION_KAFKA_CONSUMER_GROUP`

## Docker
```bash
cd services/notification-service
docker build -t notification-service:local .
```
