# Notification-service

Microservicio Spring Boot independiente del bounded context **Notification**.

## Responsabilidad
- Registrar y emitir notificaciones de eventos finales de órdenes.

## Ejecutar local
```bash
cd services/notification-service
mvn spring-boot:run
```

## Variables de entorno clave
- `KAFKA_BOOTSTRAP_SERVERS`
- `NOTIFICATION_DB_URL`
- `NOTIFICATION_DB_USERNAME`
- `NOTIFICATION_DB_PASSWORD`

## Estructura recomendada
- `domain/`: entidades y reglas de negocio
- `application/`: casos de uso y puertos
- `infrastructure/`: adaptadores REST/Kafka/JPA
