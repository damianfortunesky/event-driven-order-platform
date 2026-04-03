# Shipping-service

Microservicio Spring Boot independiente del bounded context **Shipping**.

## Responsabilidad
- Gestionar preparación logística (etapa 2) tras reserva de inventario.

## Ejecutar local
```bash
cd services/shipping-service
mvn spring-boot:run
```

## Variables de entorno clave
- `KAFKA_BOOTSTRAP_SERVERS`
- `SHIPPING_DB_URL`
- `SHIPPING_DB_USERNAME`
- `SHIPPING_DB_PASSWORD`

## Estructura recomendada
- `domain/`: entidades y reglas de negocio
- `application/`: casos de uso y puertos
- `infrastructure/`: adaptadores REST/Kafka/JPA
