# ADR 0002: Outbox transaccional

## Estado
Aceptado

## Decisión
Cada productor persistirá estado de negocio + outbox en la misma transacción.

## Consecuencia
Se minimiza el riesgo de dual-write inconsistente DB/Kafka.
