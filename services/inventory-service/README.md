# inventory-service

Servicio del bounded context **Inventory** para reserva de stock por orden.

## Objetivo

Garantizar consistencia de stock y evitar sobreventa cuando una orden ya fue aprobada en pago.

## Responsabilidades

- Consumir `PaymentApproved`.
- Verificar disponibilidad de productos.
- Reservar stock en transacción.
- Publicar `InventoryReserved` o `InventoryFailed`.
- Evitar duplicados por inbox/idempotencia.

## Eventos

### Consume
- `payments.payment-approved.v1`

### Produce
- `inventory.inventory-reserved.v1`
- `inventory.inventory-failed.v1`

## Persistencia

Tablas principales:
- `inventory_stock`
- `inventory_reservation`
- `inventory_reservation_item`
- `inventory_inbox_event`

## Garantías técnicas

- Lock pesimista para proteger concurrencia.
- Restricción de no stock negativo.
- Estrategia retry + DLQ para errores transitorios.

## Variables de entorno clave

- `KAFKA_BOOTSTRAP_SERVERS`
- `INVENTORY_DB_URL`
- `INVENTORY_DB_USERNAME`
- `INVENTORY_DB_PASSWORD`

## Ejecutar local

```bash
cd services/inventory-service
mvn spring-boot:run
```
