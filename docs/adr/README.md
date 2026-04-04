# ADRs principales

Este índice resume las decisiones arquitectónicas más relevantes del proyecto.

## ADR activos

1. `0001-topic-naming.md`
   - Define estándar de nombres y versionado de tópicos Kafka.
2. `0002-outbox-pattern.md`
   - Establece outbox pattern para consistencia entre DB y publicación de eventos.
3. `0003-retry-dlq-strategy.md`
   - Define estrategia de retries, backoff y Dead Letter Queue.

## Criterios de gobernanza ADR

- Todo cambio arquitectónico significativo debe registrar ADR.
- Un ADR no se elimina: se reemplaza/supersede con uno nuevo.
- Debe incluir contexto, decisión, consecuencias y alternativas.
