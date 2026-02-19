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



