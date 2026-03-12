# Project Walkthrough

- [Project Walkthrough](#project-walkthrough)
  - [Project Goal](#project-goal)
  - [APIs](#apis)
  - [High level architecture](#high-level-architecture)
  - [Data Model](#data-model)
  - [Code Walkthrough](#code-walkthrough)
  - [Decisions](#decisions)
  - [Future](#future)


## Project Goal

A foreign exchange rate service where users can:
- Get the list of supported currencies
- Get all available exchange rates
- Get exchange rates for a specific date
- Convert a foreign currency amount to EUR

## APIs

| Method | Endpoint                | Params                             |
| ------ | ----------------------- | -----------------------------------|
| GET    | /api/currencies         | none                               |
| GET    | /api/rates              | start+end, currency, limit, offset |
| GET    | /api/rates/{date}       | path: date                         |
| GET    | /api/conversions/to-eur | date, currency, amount             |
| POST   | /api/update             | start, end                         |

## High level architecture


    +----------------------------+
    |     CurrencyController     |
    |        REST endpoints      |
    +-------------+--------------+
                  |
                  v
    +----------------------------+
    |        Service Layer       |
    | CurrencyService            |
    | CurrencyConversionService  |
    +-------------+--------------+
                  |
                  v
    +----------------------------+
    |         Sync Layer         |
    |        SyncService         |
    |                            |
    +-------------+--------------+
                    |
            +-------+-------+
            |               |
            v               v
    +-------------+   +------------------+
    | H2 Database  |   |   Bank Service   |
    | exchange_rate|   |  orchestration   |
    | local cache  |   +--------+---------+
    +------+------+             |
            ^                   v
            |          +-------------------+
            |          |  BankRestClient   |
            |          | Bundesbank HTTP   |
            |          +--------+----------+
            |                   |
            |                   v
            |          +-------------------+
            |          | BankResponseParser|
            |          | SDMX JSON -> rows |
            |          +--------+----------+
            |                   |
            +-------------------+
                    rows persisted

## Data Model

    +-----------------------------------+
    |         exchange_rate             |
    +-----------------------------------+
    | date        | LocalDate           |
    | currency    | String              |
    | rate        | BigDecimal          |
    +-----------------------------------+
    | Primary Key: (date, currency)     |
    +-----------------------------------+

## Code Walkthrough

/rates

/conversions/to-eur 


## Decisions

- Persist rates locally and read from DB
  - Sync on read paths
- Simple lock for sync concurrency
- Simple DB query


## Future
-  better error responses.
-  better sync completeness checks beyond date-only coverage
-  retry/backoff and operational metrics for sync/external calls.
-  rate limiter for external API requests
-  persistence integration tests to validate query correctness and paging behavior end-to-end.

