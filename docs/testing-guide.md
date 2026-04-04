# Testing Guide

## Objetivo

Validar funcionalidad, resiliencia e interoperabilidad de un sistema distribuido orientado a eventos.

## Pirámide de pruebas recomendada

1. **Unit tests**
   - Reglas de negocio puras por servicio.
2. **Integration tests**
   - Repositorios + Kafka + serialización + migraciones.
3. **Contract tests**
   - Compatibilidad de schemas/event envelopes.
4. **E2E tests**
   - Flujo completo: `OrderCreated -> resultado final`.
5. **Chaos/failure tests**
   - Reintentos, DLQ, idempotencia, caídas parciales.

## Casos críticos mínimos

- Pago aprobado + inventario reservado -> orden confirmada.
- Pago rechazado -> orden cancelada + notificación.
- Pago aprobado + inventario insuficiente -> orden cancelada + notificación.
- Evento duplicado -> no duplicar side effects.
- Evento inválido -> manejo en DLQ según política.

## Ejecución local sugerida

```bash
make up
make topics

# Por servicio
cd services/payment-service && mvn test
cd services/inventory-service && mvn test
# repetir por cada servicio
```

## Criterios de aceptación

- Tasa de éxito estable en flujos nominales.
- DLQ bajo control y con causa clasificada.
- Sin violaciones de contratos de eventos.
- Métricas/alertas con señal útil para operación.

## Métricas a observar durante testing

- Throughput por tópico.
- Consumer lag por grupo.
- Tiempo de procesamiento por evento.
- Tasa de retries y volumen DLQ.
