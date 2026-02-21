# Development Notes


# API Design

Derive APIs based on requirements

1. Get a list of all available currencies


```json
GET /currencies
-> {
  "base": "EUR",
  "currencies": ["USD","GBP","JPY"],
}
```

2. Get all EUR-FX exchange rates at all available dates as a collection


```json
GET /rates
GET /rates?start&end&currency&limit&offset
GET /rates?start=YYYY-MM-DD&end=YYYY-MM-DD&currency=USD&limit=1000&offset=0
-> 
{
  "base": "EUR",
  "start": "2026-01-01",
  "end": "2026-01-31",
  "items": [
    {"date":"2026-01-02","currency":"USD","rate":1.0923},
    {"date":"2026-01-02","currency":"GBP","rate":0.8541},
    {"date":"2026-01-03","currency":"USD","rate":1.0888}
  ],
  "page": {"limit": 1000, "offset": 0, "total": 123456}
}
```

400 -> invalid input

- Pagination
- Ordering by date and currency
- Default limit 1000, max 5000
- Empty items array for no results

3. Get the EUR-FX exchange rate at particular day

```json
GET /rates/{date}
GET /rates/{date}?currency=USD   (optional)
-> 
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
400 -> Invalid date format
404 -> No rate exists for that date


4. Get a foreign exchange amount for a given currency converted to EUR on a particular day

```json
GET /conversions/to-eur?date&currency&amount
-> 
    {
  "date": "2026-02-18",
  "base": "EUR",
  "from": { "currency": "USD", "amount": 100.00 },
  "to": { "currency": "EUR", "amount": 91.56 },
  "rate": { "pair": "EUR/USD", "value": 1.0923 }
}
```

400 -> invalid input
404 -> no rate found

TODO:
- API versioning 

# Exchange rate client query

Query the exchange rate data from the bundesbank

SDMX = Statistical Data and Metadata eXchange

An international standard for Exchanging statistical data between institutions used by Banks etc.

Bundesbank SDMX Webservice endpoint

https://api.statistiken.bundesbank.de/doc/index.html?urls.primaryName=Deutsche+REST+API+Dokumentation

Documentation on the GET endpoint needed to get EUR-FX data

https://www.bundesbank.de/dynamic/action/en/statistics/time-series-databases/help-for-sdmx-web-service/web-service-interface-data/855914/web-service-interface-data

Base API URL:

https://api.statistiken.bundesbank.de/

REST End point to get the statstical time series data

/rest/data/{flowRef}/{key}


Dataflow ID/ flowRef =  BBEX3

Key = D..EUR.BB.AC.000

Key meanings

- D
Frequency = Daily exchange rates.

- . (empty second position)

All currencies as leaving a dimension empty is a wildcard.

So instead of:

D.USD.EUR.BB.AC.000
D.GBP.EUR.BB.AC.000

Give all currencies vs EUR.

- EUR

This is the base currency.

Rates are expressed as:
1 EUR = X foreign currency

- BB

Source identifier from Bundesbank dataset.

- AC

Rate type AC = Average rate

- 000 Code


Usage guide:

URL: https://api.statistiken.bundesbank.de/rest/data/{flowRef}/{key}?startPeriod=YYYY-MM-DD&endPeriod=YYYY-MM-DD&format=sdmx_json

URL: https://api.statistiken.bundesbank.de/rest/data/BBEX3/D..EUR.BB.AC.000?format=sdmx_json

This can cause a long wait time, so need to the specify dates to keep response time managable.

https://api.statistiken.bundesbank.de/rest/data/BBEX3/D..EUR.BB.AC.000?startPeriod=2026-01-01&endPeriod=2026-01-31&format=sdmx_json

Implemented a working REST Client Call to retrieve exchange rates from BB


# Data model

Define the data model for storing the data in the DB, before diving into the execution flow.


Table for storing rate data

┌───────────────────────────────────────┐
│            exchange_rate              │
├───────────────────────────────────────┤
│ rate_date   DATE        (PK)          │
│ currency    VARCHAR(3)  (PK)          │
│ rate        DECIMAL(19,8)             │
└───────────────────────────────────────┘

Primary Key: (rate_date, currency)


```sql
CREATE TABLE exchange_rate (
    rate_date      DATE        NOT NULL,
    currency       VARCHAR(3)  NOT NULL,
    rate           DECIMAL(19,8) NOT NULL,

    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_exchange_rate PRIMARY KEY (rate_date, currency)
);

CREATE INDEX idx_exchange_rate_currency ON exchange_rate(currency);
CREATE INDEX idx_exchange_rate_date ON exchange_rate(rate_date);
```


# Get Currency Logic Flow 


Boot sequence

- Check sync state:
  max(rate_date) in exchange_rate
- Default time window for checking last 30 days
- Check if data missing within that window
- Send request to BB to get missing data
- Store in DB
- Continue Boot

Get currencies
- Controller receives request.
- Service checks if the data is outdated
  - if sync recently do nothing
  - otherwise request to BB to update db
- Query currenies from the DB
- Return list


# Sync Service

Service responsible for requesting new data via bank service and updating the database

# User story 1 implementation 

Setup the end to end flow for get Currency

1. Add the H2 and Spring Data JPA dependencies in pom.xml
2. Setup the h2 configuration in application.properties
3. BankService is responsibile for client call to the Bundesbank to fetch data
4. ExchangeRateRow represents entity (date, currency, rate) returned by BankService as a list
5. Define the data model:
   - Table: exchange_rate
   - ExchangeRateId (date and currency) make up the composite primary key of the table
   - ExchangeRateEntity is the data model entity with ExchangeRateId and rate
   - ExchangeRateRepository is the JPA repository with defined database queries
6. SyncService responsible for requesting new data with BankService and updating the database
7. CurrencyService is responsible for handling the requests from the CurrencyController

# User story 2: rates

1. DTO for response
2. Extend the JPA repository
   - count of rates
   - get rates with query params
3. Extend the Currency Service to query db
4. /rates in the Currency Controller


# User story 3: rates of a single day

```json
GET /rates/{date}
GET /rates/{date}?currency=USD
```

1. DTO for response
2. Extend the JPA repository
   - find rate by date with currency as option
3. Extend the Currency Service to query db
4. /rates/date in the Currency Controller
5. add Rest controller advice for error handling

# Final User story 4: foreign currency amount to eur conversion

1. DTO for response
2. Extend the JPA repository
   - find rate for composite primary key of date and currency
3. New Conversion Service to handle currency conversion
4. new endpoint for this feature
  GET /conversions/to-eur?date&currency&amount

# Startup

Add Sync on boot

# Docker

- Add docker and compose files
- Added info and health actuator endpoints

