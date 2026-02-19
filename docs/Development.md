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
GET /v1/rates/{date}?currency=USD   (optional)
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

