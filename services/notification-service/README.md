# notification-service

Servicio del bounded context **Notifications** para comunicación de estado final al cliente.

## Objetivo

Centralizar la emisión y persistencia de notificaciones derivadas de eventos finales del proceso de orden.

## Responsabilidades

- Consumir eventos finales del flujo.
- Generar contenido de notificación por plantilla.
- Persistir historial de notificaciones.
- Evitar duplicados por llaves idempotentes.

## Eventos

### Consume
- `orders.order-confirmed.v1`
- `orders.order-cancelled.v1`
- `payments.payment-rejected.v1`
- `inventory.inventory-failed.v1`

### Produce
- Opcional: evento interno/auditoría de notificación emitida.

## API opcional

- `GET /api/notifications`
- `GET /api/notifications?orderId=...`

## Variables de entorno clave

- `KAFKA_BOOTSTRAP_SERVERS`
- `NOTIFICATION_DB_URL`
- `NOTIFICATION_DB_USERNAME`
- `NOTIFICATION_DB_PASSWORD`

## Ejecutar local

```bash
cd services/notification-service
mvn spring-boot:run
```
