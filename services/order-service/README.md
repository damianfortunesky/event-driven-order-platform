# Order-service

Microservicio Spring Boot independiente del bounded context **Order**.

## Responsabilidad
- Gestionar ciclo de vida de órdenes y orquestación ligera por eventos.

## Ejecutar local
```bash
cd services/order-service
mvn spring-boot:run
```

## Variables de entorno clave
- `KAFKA_BOOTSTRAP_SERVERS`
- `ORDER_DB_URL`
- `ORDER_DB_USERNAME`
- `ORDER_DB_PASSWORD`

## Estructura recomendada
- `domain/`: entidades y reglas de negocio
- `application/`: casos de uso y puertos
- `infrastructure/`: adaptadores REST/Kafka/JPA
