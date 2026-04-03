# Payment-service

Microservicio Spring Boot independiente del bounded context **Payment**.

## Responsabilidad
- Procesar pagos a partir de órdenes creadas y emitir resultados.

## Ejecutar local
```bash
cd services/payment-service
mvn spring-boot:run
```

## Variables de entorno clave
- `KAFKA_BOOTSTRAP_SERVERS`
- `PAYMENT_DB_URL`
- `PAYMENT_DB_USERNAME`
- `PAYMENT_DB_PASSWORD`

## Estructura recomendada
- `domain/`: entidades y reglas de negocio
- `application/`: casos de uso y puertos
- `infrastructure/`: adaptadores REST/Kafka/JPA
