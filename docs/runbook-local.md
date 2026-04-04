# Runbook Local

## Objetivo

Levantar un entorno local reproducible para desarrollo y pruebas end-to-end de flujo de órdenes por eventos.

## Prerrequisitos

- Docker + Docker Compose
- Java 21
- Maven 3.9+
- GNU Make

## 1) Bootstrap inicial

```bash
cp .env.example .env
make bootstrap
```

## 2) Levantar infraestructura

```bash
make up
make ps
```

Servicios esperados: Kafka, PostgreSQLs requeridos y componentes de observabilidad definidos en compose.

## 3) Crear tópicos

```bash
make topics
```

## 4) Levantar microservicios

En terminales separadas:

```bash
cd services/order-service && mvn spring-boot:run
cd services/payment-service && mvn spring-boot:run
cd services/inventory-service && mvn spring-boot:run
cd services/shipping-service && mvn spring-boot:run
cd services/notification-service && mvn spring-boot:run
```

## 5) Verificar salud

- Revisar logs de cada servicio.
- Confirmar conexión a Kafka y migraciones Flyway.
- Validar actividad en tópicos con herramientas Kafka CLI/UI.

## 6) Apagado limpio

```bash
make down
```

> `down -v` elimina volúmenes y estado local persistido.
