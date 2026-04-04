# Inventory-service

Microservicio Spring Boot 3 + Java 21 para el bounded context **Inventory**.

## Responsabilidad
- Mantener stock disponible por producto.
- Consumir eventos `PaymentApproved`.
- Intentar reservar stock de los ítems de la orden.
- Publicar `InventoryReserved` o `InventoryFailed`.
- Evitar procesamiento duplicado con inbox/idempotencia.

## Modelo de datos (PostgreSQL)
Tablas gestionadas por Flyway:

- `inventory_stock`: stock disponible por `product_id`.
- `inventory_reservation`: resultado de cada intento de reserva por `payment_event_id`/`order_id`.
- `inventory_reservation_item`: ítems reservados/fallidos por reserva.
- `inventory_inbox_event`: eventos consumidos para idempotencia.

## Migraciones Flyway
- `V1__create_inventory_schema.sql`: crea todas las tablas, PK/FK e índices.
- `V2__seed_inventory_stock.sql`: inserta stock de prueba inicial.

## Kafka
### Consumer
- Topic de entrada: `payments.payment-approved.v1`.
- Grupo: `inventory-service-group` (configurable).
- Retries con `DefaultErrorHandler + ExponentialBackOffWithMaxRetries`.
- DLQ automática en `<topic>.dlq` cuando se agotan reintentos.

### Producer
- Publica `InventoryReserved` en `inventory.inventory-reserved.v1`.
- Publica `InventoryFailed` en `inventory.inventory-failed.v1`.

## Estrategia de idempotencia
1. Se verifica si el `eventId` ya existe en `inventory_inbox_event`.
2. Se verifica si ya existe una `inventory_reservation` para el `payment_event_id` o para el `order_id`.
3. Si no existe, se procesa en transacción única (bloqueo pesimista de stock + reserva + inbox).
4. Si llega duplicado (mismo `eventId` o mismo `orderId`), se reutiliza el resultado persistido y se evita duplicar descuento de stock.

## Concurrencia y regla de no stock negativo
- Lock pesimista (`PESSIMISTIC_WRITE`) al leer filas de `inventory_stock`.
- Validación previa de todas las cantidades solicitadas.
- Descuento de stock en la misma transacción.
- Restricción SQL `CHECK (available_quantity >= 0)`.

## Inicializar stock de prueba
La migración `V2__seed_inventory_stock.sql` crea:

- `11111111-1111-1111-1111-111111111111` -> stock 10
- `22222222-2222-2222-2222-222222222222` -> stock 3
- `33333333-3333-3333-3333-333333333333` -> stock 0

También podés insertar más stock manualmente:

```sql
INSERT INTO inventory_stock(product_id, available_quantity)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 25)
ON CONFLICT (product_id)
DO UPDATE SET available_quantity = EXCLUDED.available_quantity,
              updated_at = now();
```

## Ejecutar local
```bash
cd services/inventory-service
mvn spring-boot:run
```

## Variables de entorno
- `KAFKA_BOOTSTRAP_SERVERS`
- `INVENTORY_DB_URL`
- `INVENTORY_DB_USERNAME`
- `INVENTORY_DB_PASSWORD`
- `PAYMENT_APPROVED_TOPIC`
- `INVENTORY_RESERVED_TOPIC`
- `INVENTORY_FAILED_TOPIC`
