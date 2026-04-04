# shipping-service

Servicio del bounded context **Shipping** para la etapa logística posterior a la reserva de inventario.

## Objetivo

Preparar el despacho de órdenes confirmables y publicar hitos logísticos para seguimiento operativo.

## Responsabilidades

- Consumir eventos de inventario reservado.
- Crear preparación logística/envío.
- Publicar eventos de estado logístico (cuando aplique).
- Permitir trazabilidad operacional del fulfillment.

## Eventos

### Consume
- `inventory.inventory-reserved.v1`

### Produce (sugerido)
- `shipping.shipment-prepared.v1`
- `shipping.shipment-dispatched.v1`

## Variables de entorno clave

- `KAFKA_BOOTSTRAP_SERVERS`
- `SHIPPING_DB_URL`
- `SHIPPING_DB_USERNAME`
- `SHIPPING_DB_PASSWORD`

## Ejecutar local

```bash
cd services/shipping-service
mvn spring-boot:run
```
