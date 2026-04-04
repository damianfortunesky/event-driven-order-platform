# Event-Driven Order Platform

Plataforma de referencia para procesamiento de órdenes usando **arquitectura orientada a eventos** con **Kafka**, **microservicios Spring Boot** y **PostgreSQL por servicio**.

> Este repositorio está diseñado para ser entendido y operado como un proyecto real de equipo: incluye contratos de eventos, arquitectura, ADRs, guías de operación y documentación de despliegue/testing.

## Objetivo del sistema

Orquestar el ciclo de vida de una orden de compra de forma desacoplada:

1. Se crea la orden.
2. Se evalúa el pago.
3. Se reserva inventario.
4. Se confirma o cancela la orden.
5. Se notifica al cliente.

El foco es priorizar:
- **Desacoplamiento temporal y funcional** entre servicios.
- **Trazabilidad end-to-end** con `correlationId`.
- **Resiliencia operativa** (retries, DLQ, idempotencia).
- **Escalabilidad horizontal** por particiones y consumer groups.

## Arquitectura elegida

- **Estilo**: Microservicios + Event-Driven Architecture.
- **Broker**: Apache Kafka.
- **Persistencia**: PostgreSQL por bounded context.
- **Stack app**: Java 21 + Spring Boot 3.
- **Infra local**: Docker Compose.
- **Observabilidad**: Prometheus/Grafana + logs estructurados.

Documentación de arquitectura:
- [Architecture Overview](docs/architecture-overview.md)
- [C4 model simplificado](docs/c4-model-simplificado.md)
- [Event Catalog](docs/event-catalog.md)
- [ADRs principales](docs/adr/README.md)

## Responsabilidades por servicio

- `order-service`: ciclo de vida de la orden, publicación de eventos de negocio.
- `payment-service`: autorización/rechazo de pago.
- `inventory-service`: reserva de stock y validación de disponibilidad.
- `shipping-service`: preparación logística posterior a reserva.
- `notification-service`: notificaciones finales al cliente.

Ver detalle por servicio en:
- `services/order-service/README.md`
- `services/payment-service/README.md`
- `services/inventory-service/README.md`
- `services/shipping-service/README.md`
- `services/notification-service/README.md`

## Flujo de eventos (alto nivel)

```text
OrderCreated
  -> PaymentApproved | PaymentRejected
       -> (si approved) InventoryReserved | InventoryFailed
            -> (si reserved) OrderConfirmed
            -> (si failed)   OrderCancelled
       -> (si rejected)      OrderCancelled

OrderConfirmed / OrderCancelled / PaymentRejected / InventoryFailed
  -> NotificationSent (interno de notification-service)
```

## Estructura del repositorio

- `services/`: microservicios por bounded context.
- `contracts/`: schemas, envelopes y catálogo de tópicos.
- `docs/`: arquitectura, ADRs, runbooks y guías operativas.
- `infra/`: bootstrap local (Kafka, PostgreSQL, compose).
- `deploy/`: lineamientos de despliegue.
- `observability/`: configuración base de monitoreo.
- `scripts/`: automatizaciones de entorno y validaciones.

## Guía rápida local

```bash
cp .env.example .env
make bootstrap
make up
make topics
make ps
```

Referencias:
- [Runbook local](docs/runbook-local.md)
- [Troubleshooting guide](docs/troubleshooting-guide.md)
- [Testing guide](docs/testing-guide.md)
- [Deployment guide](docs/deployment-guide.md)

## Convenciones clave

- Tópicos Kafka: `<bounded-context>.<event-name>.v<version>`
  - ejemplo: `orders.order-created.v1`
- Clave de partición: `orderId`.
- Eventos de dominio: `PascalCase`.
- Variables de entorno: `UPPER_SNAKE_CASE`.
- Versionado de contrato: backward-compatible cuando sea posible.

## Estado del repositorio

Base profesional lista para:
- implementación incremental de casos de uso,
- pruebas de integración orientadas a eventos,
- operación local reproducible,
- evolución guiada por ADRs.
