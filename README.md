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


## Documentation:
- Objective: `docs/Objective.md`
- Development notes: `docs/Development.md`

## Implemented User Stories

- List of all available currencies
- Get all EUR-FX exchange rates as a collection (with filtering and pagination)
- Get EUR-FX exchange rates for a particular day
- Convert a foreign currency amount to EUR on a particular day

## Overview

- On startup, the service triggers an initial sync from Bundesbank into the local H2 database.
- For get endpoints it ensures relevant data is synced or fetch new data, then reads from DB and returns DTO responses.
- Post update api forces a manual sync for a specific date range if needed to writing fresh rates to DB.

## Tech Stack
- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database
- Maven
- Springdoc OpenAPI (Swagger UI)
- Docker / Docker Compose

## API Endpoints

- Base URL: `http://localhost:8080`
- API base path: `/api`

| Method | Endpoint                  | Params                                   |
| ------ | ------------------------- | ---------------------------------------- |
| GET    | /api/currencies           | none                                     |
| GET    | /api/rates                | start, end, currency, limit, offset      |
| GET    | /api/rates/{date}         | path: date                               |
| GET    | /api/conversions/to-eur   | date, currency, amount                   |
| POST   | /api/update               | start, end                               |

## API Examples

### 1) List currencies

```bash
curl "http://localhost:8080/api/currencies"
```

```json
["AUD","CAD","CHF","GBP","JPY","USD"]
```

### 2) Rates collection

```bash
curl "http://localhost:8080/api/rates?start=2026-01-01&end=2026-01-31&currency=USD&limit=3&offset=0"
```

```json
{
  "base": "EUR",
  "start": "2026-01-01",
  "end": "2026-01-31",
  "items": [
    {"date":"2026-01-02","currency":"USD","rate":1.0923},
    {"date":"2026-01-05","currency":"USD","rate":1.0888},
    {"date":"2026-01-06","currency":"USD","rate":1.0910}
  ],
  "page": {"limit":3,"offset":0,"total":22}
}
```

### 3) Rates for a specific day

```bash
curl "http://localhost:8080/api/rates/2026-02-18"
```

```json
{
  "base": "EUR",
  "date": "2026-02-18",
  "rates": {
    "USD": 1.0923,
    "GBP": 0.8541,
    "JPY": 161.22
  }
}
```

### 4) Convert foreign amount to EUR

```bash
curl "http://localhost:8080/api/conversions/to-eur?date=2026-02-18&currency=USD&amount=100.00"
```

```json
{
  "date": "2026-02-18",
  "base": "EUR",
  "from": {"currency":"USD","amount":100.00},
  "to": {"currency":"EUR","amount":91.56},
  "rate": {"pair":"EUR/USD","value":1.0923}
}
```

## Testing

Run tests:

```bash
cd service && ./mvnw test
```


## Notes and Limitations
- Uses H2 file database for assignment simplicity.
- Sync strategy currently fetches from Bundesbank and upserts into local DB.

## Future improvements:
  - Persist sync time in a dedicated sync-time table.
  - A rate limiter for external API requests

## AI Usage

AI usage details are documented in `docs/AI_USAGE.md`.
