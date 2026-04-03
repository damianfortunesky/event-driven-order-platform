# Event-Driven Order Platform

Monorepo base para una plataforma **Kafka + Event-Driven Architecture** con microservicios Spring Boot independientes.

## Objetivo
Construir una base profesional, limpia y escalable para evolucionar un flujo de órdenes desacoplado por eventos.

## Stack base
- Java 21 + Spring Boot 3.x
- Apache Kafka
- PostgreSQL por microservicio
- Docker Compose para entorno local
- Kubernetes manifests base para despliegue
- Observabilidad con Prometheus + Grafana

## Estructura
- `services/`: microservicios Spring Boot independientes
- `contracts/`: contratos de eventos (schemas/envelopes/topic definitions)
- `infra/`: infraestructura local (docker compose, kafka, postgres)
- `deploy/`: manifests base para Kubernetes
- `docs/`: arquitectura, ADRs, diagramas y runbooks
- `scripts/`: automatizaciones operativas y de validación
- `observability/`: configuración de métricas, dashboards y alertas

## Convenciones de nombres
- Repositorio: `event-driven-order-platform`
- Servicios: `kebab-case` con sufijo `-service` (ej: `order-service`)
- Tópicos Kafka: `<bounded-context>.<event-name>.v<version>`
  - Ejemplo: `orders.order-created.v1`
- Eventos de negocio: `PascalCase` (ej: `OrderCreated`)
- Clave de partición: `orderId` para mantener ordering por agregado
- Variables de entorno: `UPPER_SNAKE_CASE`

## Estrategia de variables de entorno
1. `.env.example` define defaults y nombres oficiales.
2. `.env` (local, no versionado) contiene secretos/override.
3. Cada servicio lee variables con prefijo propio (`ORDER_`, `PAYMENT_`, etc.).
4. En Kubernetes, variables via `ConfigMap` + `Secret`.

## Inicio rápido
```bash
cp .env.example .env
make bootstrap
make up
make topics
```

## Próximos pasos sugeridos
1. Inicializar cada microservicio con Spring Initializr.
2. Implementar modelo de dominio y puertos hexagonales.
3. Agregar outbox transaccional en productores.
4. Integrar consumidores Kafka con retries y DLT.
5. Completar observabilidad distribuida.
