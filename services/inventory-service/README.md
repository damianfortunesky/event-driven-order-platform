# Inventory-service

Microservicio Spring Boot independiente del bounded context **Inventory**.

## Responsabilidad
- Reservar/liberar inventario y publicar resultado de stock.

## Ejecutar local
```bash
cd services/inventory-service
mvn spring-boot:run
```

## Variables de entorno clave
- `KAFKA_BOOTSTRAP_SERVERS`
- `INVENTORY_DB_URL`
- `INVENTORY_DB_USERNAME`
- `INVENTORY_DB_PASSWORD`

## Estructura recomendada
- `domain/`: entidades y reglas de negocio
- `application/`: casos de uso y puertos
- `infrastructure/`: adaptadores REST/Kafka/JPA
