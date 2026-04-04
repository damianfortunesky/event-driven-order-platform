# order-service

Servicio del bounded context **Orders** responsable del ciclo de vida de la orden.

## Objetivo

Crear y evolucionar el estado de una orden en función de eventos de pago e inventario, publicando eventos finales de negocio.

## Responsabilidades

- Crear órdenes (`OrderCreated`).
- Consumir resultados de pago/inventario.
- Confirmar o cancelar órdenes (`OrderConfirmed`, `OrderCancelled`).
- Mantener trazabilidad por `orderId` y `correlationId`.

## Eventos

### Produce
- `orders.order-created.v1`
- `orders.order-confirmed.v1`
- `orders.order-cancelled.v1`

### Consume
- `payments.payment-approved.v1`
- `payments.payment-rejected.v1`
- `inventory.inventory-reserved.v1`
- `inventory.inventory-failed.v1`

## Datos

- Base dedicada de órdenes.
- Estados sugeridos: `CREATED`, `PAYMENT_REJECTED`, `INVENTORY_FAILED`, `CONFIRMED`, `CANCELLED`.

## Variables de entorno clave

- `KAFKA_BOOTSTRAP_SERVERS`
- `ORDER_DB_URL`
- `ORDER_DB_USERNAME`
- `ORDER_DB_PASSWORD`

## Ejecutar local

```bash
cd services/order-service
mvn spring-boot:run
```
