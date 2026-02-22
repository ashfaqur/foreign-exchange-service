# foreign-exchange-service

A Spring Boot microservice providing EUR-based foreign exchange reference rates.


## Run with Docker (recommended)

```bash
docker compose build
docker compose up
```

## Run Manually

Requirements:
- Java 21
- Maven 3

```bash
cd service
./mvnw spring-boot:run
```

## Service

Service runs by default on: `http://localhost:8080`
Swagger UI: http://localhost:8080/swagger-ui/index.html



