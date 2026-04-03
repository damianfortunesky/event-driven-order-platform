# Runbook local development (Docker Compose)

## 1) Preparación

```bash
cp .env.example .env
```

> Ajustá valores en `.env` si necesitás cambiar puertos, credenciales o nombre del proyecto.

## 2) Levantar todo el entorno

```bash
docker compose --env-file .env up -d
```

Alternativa:

```bash
make up
```

## 3) Crear/verificar topics de Kafka

Se puede ejecutar manualmente:

```bash
make topics
```

También podés revisar el servicio `kafka-topics-init` en logs para confirmar que corrió correctamente.

## 4) Validaciones de Kafka operativo

### Validación A: health del contenedor

```bash
docker compose --env-file .env ps
```

Esperado: `kafka` en estado `healthy`.

### Validación B: listar topics desde Kafka

```bash
docker compose --env-file .env exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
```

Esperado: aparecen los topics de negocio (`orders.order-created.v1`, etc.).

### Validación C: probar acceso desde host local

```bash
docker compose --env-file .env exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:19092 --list
```

> Nota: dentro del contenedor Kafka el listener externo está en `localhost:19092`, que se publica en el host como `localhost:${KAFKA_EXTERNAL_PORT}` (por defecto `9092`).

## 5) Puertos expuestos

- Kafka externo (host): `${KAFKA_EXTERNAL_PORT}` (default `9092`)
- Kafka UI: `${KAFKA_UI_PORT}` (default `8080`)
- Postgres order: `${POSTGRES_ORDER_PORT}` (default `5432`)
- Postgres payment: `${POSTGRES_PAYMENT_PORT}` (default `5433`)
- Postgres inventory: `${POSTGRES_INVENTORY_PORT}` (default `5434`)
- Postgres notification: `${POSTGRES_NOTIFICATION_PORT}` (default `5435`)
- Prometheus: `${PROMETHEUS_PORT}` (default `9090`)
- Grafana: `${GRAFANA_PORT}` (default `3000`)

## 6) Apagar y limpiar

```bash
docker compose --env-file .env down -v
```

Alternativa:

```bash
make down
```

## 7) Reiniciar

```bash
make restart
```

o en comandos directos:

```bash
docker compose --env-file .env down
docker compose --env-file .env up -d
```
