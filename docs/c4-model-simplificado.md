# C4 Model Simplificado

## Nivel 1 — Contexto

**Sistema**: Event-Driven Order Platform

**Actores externos**:
- Cliente (usuario final)
- Operador/Soporte
- Sistemas externos (potenciales: ERP, pasarela real de pagos, provider logístico)

**Relaciones**:
- El cliente genera órdenes y recibe notificaciones.
- Soporte monitorea estado, DLQ y trazas.
- Sistemas externos pueden integrarse consumiendo/publicando eventos.

## Nivel 2 — Contenedores

1. **order-service** (Spring Boot)
2. **payment-service** (Spring Boot)
3. **inventory-service** (Spring Boot)
4. **shipping-service** (Spring Boot)
5. **notification-service** (Spring Boot)
6. **Kafka Cluster** (mensajería/event backbone)
7. **PostgreSQL por servicio** (persistencia)
8. **Prometheus/Grafana** (observabilidad)

## Nivel 3 — Componentes (ejemplo en order-service)

- **API Adapter**: endpoints para creación/consulta de órdenes.
- **Application Services**: casos de uso (`CreateOrder`, `HandlePaymentResult`, etc.).
- **Domain Model**: entidades y reglas de negocio de órdenes.
- **Persistence Adapter**: repositorios JPA/Flyway.
- **Messaging Adapter**: publisher/consumer Kafka.

## Nivel 4 — Código

No se detalla en C4 simplificado. Se recomienda complementar con:
- diagramas de secuencia (`docs/diagrams/event-flow-sequence.mmd`),
- y ADRs (`docs/adr/`).
