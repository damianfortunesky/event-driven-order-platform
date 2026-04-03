# ADR 0003: Estrategia de retry y DLQ

## Estado
Aceptado

## Decisión
Aplicar retries exponenciales en consumidores y derivar a DLT al agotar intentos.

## Consecuencia
Aumenta resiliencia y facilita reproceso controlado.
