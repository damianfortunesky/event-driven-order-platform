# Troubleshooting Guide

## 1) El servicio no inicia

### Síntomas
- Error de conexión a DB o Kafka.

### Verificaciones
1. `make ps` y revisar contenedores en estado `Up`.
2. Validar variables en `.env`.
3. Revisar puertos ocupados localmente.

### Acciones
- Reiniciar infraestructura: `make restart`.
- Si hay corrupción de estado local: `make down` y luego `make up`.

---

## 2) No se crean/consumen eventos

### Síntomas
- No hay progreso en flujo de órdenes.

### Verificaciones
1. Ejecutar `make topics`.
2. Confirmar nombres de tópicos configurados por servicio.
3. Revisar consumer group y offsets.

### Acciones
- Corregir topic mapping y reiniciar servicio afectado.
- Revisar que la clave Kafka (`orderId`) esté presente.

---

## 3) Crecimiento de DLQ

### Síntomas
- Mensajes acumulados en `<topic>.dlq`.

### Verificaciones
1. Identificar causa raíz (schema inválido, dato incompleto, bug de negocio).
2. Clasificar si es evento puntual o incidente sistémico.

### Acciones
- Corregir productor/consumidor según causa.
- Reprocesar mensajes con procedimiento controlado (ver runbooks en `docs/runbooks/`).

---

## 4) Duplicados observados

### Síntomas
- Múltiples side effects para misma orden.

### Verificaciones
1. Confirmar llaves idempotentes en tabla inbox/procesamiento.
2. Revisar políticas de retry y commit de offsets.

### Acciones
- Fortalecer deduplicación por `eventId` y/o `orderId`.
- Evitar side effects fuera de transacción protegida.

---

## 5) Latencia elevada

### Verificaciones
- Lag de consumidores.
- Saturación de particiones.
- Retries excesivos.

### Acciones
- Escalar réplicas del consumidor.
- Incrementar particiones en tópicos críticos.
- Optimizar consultas SQL y uso de locks.
