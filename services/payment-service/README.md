# payment-service

Servicio del bounded context **Payments** que decide el resultado del pago de una orden.

## Objetivo

Procesar de manera determinística los eventos de pago para habilitar o bloquear la continuidad del flujo de orden.

## Responsabilidades

- Consumir `OrderCreated`/`PaymentRequested`.
- Evaluar reglas de aprobación/rechazo.
- Persistir resultado de pago.
- Publicar `PaymentApproved` o `PaymentRejected`.
- Aplicar idempotencia por `eventId`.

## Reglas de negocio (resumen)

1. Rechazo por umbral de monto configurable.
2. Rechazo pseudoaleatorio determinístico para testing controlado.

## Eventos

### Consume
- `orders.order-created.v1`
- `payments.payment-requested.v1` (si se usa etapa intermedia)

### Produce
- `payments.payment-approved.v1`
- `payments.payment-rejected.v1`

## Operabilidad

- Retries con backoff exponencial.
- Derivación a DLQ al agotar reintentos o ante payload inválido.
- Métricas de procesamiento para observabilidad.

## Variables de entorno clave

- `KAFKA_BOOTSTRAP_SERVERS`
- `PAYMENT_DB_URL`
- `PAYMENT_DB_USERNAME`
- `PAYMENT_DB_PASSWORD`

## Ejecutar local

```bash
cd services/payment-service
mvn spring-boot:run
```
