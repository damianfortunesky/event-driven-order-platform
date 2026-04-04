# Deployment Guide

## Objetivo

Desplegar la plataforma en entornos gestionados (staging/production) con prácticas operativas seguras.

## Estrategia recomendada

- **Orquestación**: Kubernetes.
- **Configuración**: `ConfigMap` para parámetros, `Secret` para credenciales.
- **Persistencia**: PostgreSQL gestionado por entorno.
- **Mensajería**: Kafka gestionado (o cluster autogestionado con hardening).

## Pipeline sugerido

1. Build + test de cada servicio.
2. Scan de seguridad de imagen.
3. Publicación de imagen versionada (`service:gitsha`).
4. Deploy progresivo (rolling/canary).
5. Validación post-deploy (health + métricas + smoke tests).

## Checklist de release

- [ ] Contratos de eventos versionados y compatibles.
- [ ] Migraciones Flyway incluidas y revisadas.
- [ ] Variables de entorno completas por servicio.
- [ ] Alertas base habilitadas (errores, latencia, DLQ).
- [ ] Runbook de rollback validado.

## Rollback

1. Revertir deployment a versión anterior estable.
2. Pausar reprocesamientos si hay riesgo de inconsistencias.
3. Evaluar impacto en eventos pendientes/DLQ.
4. Ejecutar post-mortem y registrar ADR si aplica.

## Consideraciones de producción

- Habilitar TLS y autenticación en Kafka.
- Activar rotación de secretos.
- Definir políticas de retención por tópico según criticidad y compliance.
- Proteger endpoints de actuator en red privada.
