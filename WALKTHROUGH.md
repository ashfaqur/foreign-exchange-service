# WALKTHROUGH

## 1. Talk Track Overview (Minute-by-Minute)
- **0:00-1:00**: Problem statement and what the service delivers (EUR-based rates, date lookup, conversion, manual sync).
- **1:00-3:00**: High-level architecture and data flow from API request to stored rates.
- **3:00-5:00**: Endpoints and request/response behavior, including filters and pagination.
- **5:00-7:00**: Sync strategy, startup sync, and why read paths trigger freshness checks.
- **7:00-9:00**: Deep dive 1: sync concurrency control and update behavior.
- **9:00-11:00**: Deep dive 2: conversion precision/rounding and not-found handling.
- **11:00-13:00**: Testing story, confidence level, and known gaps.
- **13:00-15:00**: Trade-offs and “if I had more time” improvements.

## 2. High-Level Architecture Slide (Content)

### 5-box diagram (text)
`API Layer -> Service Layer -> Sync Layer -> Bank Client Layer -> Database`

- **API Layer**: `CurrencyController` receives REST requests and handles parameter binding.
- **Service Layer**: `CurrencyService` and `CurrencyConversionService` enforce business rules and response shaping.
- **Sync Layer**: `SyncService` decides when to fetch, prevents parallel syncs, and writes through `DbWriter`.
- **Bank Client Layer**: `BankService` calls `BankRestClient`, then parses SDMX payloads via `BankResponseParser`.
- **Database**: JPA repository persists/retrieves rates keyed by `(date, currency)`.

### End-to-end request flow narrative
Example: `GET /api/rates?start=2026-01-01&end=2026-01-31&currency=USD&limit=1000&offset=0`
1. `CurrencyController#getRates` binds query params.
2. `CurrencyService#getRates` validates range/pagination and normalizes currency.
3. `SyncService#syncRange(..., false)` ensures the requested range is locally available.
4. If needed, `BankService` fetches and parses external data, then `DbWriter` persists rows.
5. Repository count + paged fetch run for the filtered query.
6. Service maps entities to `RatesResponse` with `items` and `page` metadata.

## 3. Show These Components In This Order
1. **`CurrencyController`**: Show the five endpoint contracts and how each delegates to services.
2. **`CurrencyService`**: Explain range validation, default window logic, pagination handling, and date-level reads.
3. **`CurrencyConversionService`**: Explain input validation, EUR special case, and conversion rounding behavior.
4. **`SyncService`**: Show fetch-or-skip logic, force update behavior, and lock-based concurrency control.
5. **`BankRestClient` + `BankResponseParser`**: Explain external call setup, error status mapping, and tolerant parsing strategy.
6. **`ExchangeRateRepository` (+ custom implementation)**: Show optional filters, sorted retrieval, and offset pagination.
7. **`ApiExceptionHandler`**: Show how domain and validation errors become HTTP status codes.

## 4. Key Design Decisions (3-5 Items)

### Decision 1: Persist rates locally and read from DB
- **What**: Fetch external rates and store them in H2 with a composite key.
- **Why**: Fast reads, deterministic responses, and reduced external dependency for every request.
- **Trade-off**: Requires sync logic and data freshness management.
- **Next in production**: Add stronger data completeness checks and persistence integration tests.

### Decision 2: Sync on read paths
- **What**: Read endpoints call sync helpers before querying.
- **Why**: Keeps data fresher without requiring separate scheduling infrastructure.
- **Trade-off**: Read latency and availability can be affected by provider health.
- **Next in production**: Add caching window, async refresh options, and degraded-mode fallback strategy.

### Decision 3: Defensive SDMX parser
- **What**: Parser skips malformed rows instead of failing entire payload parse.
- **Why**: Preserves usable data under partial payload issues.
- **Trade-off**: Can hide upstream schema drift unless monitored.
- **Next in production**: Add strict validation thresholds and alerting when skip ratios spike.

### Decision 4: Simple lock for sync concurrency
- **What**: `ReentrantLock.tryLock()` prevents concurrent sync jobs and returns `503` behavior.
- **Why**: Minimal and safe for single-instance challenge scope.
- **Trade-off**: No distributed coordination across multiple instances.
- **Next in production**: Replace with distributed lock or queue-based sync orchestration.

## 5. Deep Dives (Pick 2)

### Deep Dive A: Sync Strategy and Concurrency Control
- `SyncService#syncRange` validates date windows and short-circuits when local coverage is considered sufficient.
- Forced sync (`POST /api/update`) bypasses coverage checks and always refetches.
- `tryLock` ensures only one sync runs at a time; concurrent attempts map to `503` via `SyncInProgressException`.
- Discussion point: date-level coverage check is a practical shortcut but can miss partial-currency gaps.

### Deep Dive B: Conversion Precision and Rounding
- `CurrencyConversionService#convertToEur` validates date/currency/amount, then syncs that day before lookup.
- Conversion uses `foreignAmount / rate` with intermediate precision and final `HALF_UP` rounding to two decimals.
- EUR special case avoids repository lookup and returns 1:1 rounded amount.
- Discussion point: rounding policy is deterministic but should be explicitly documented for client expectations.

## 6. Edge Cases & Failure Modes
- Invalid date formats in path/query -> mapped to `400` with generic invalid-input message.
- `start`/`end` one-sided or reversed -> mapped to `400` with clear validation message.
- Requested range beyond max window -> mapped to `400`.
- Missing rate for date/currency -> mapped to `404` via `RateNotFoundException`.
- Sync already running -> mapped to `503` via `SyncInProgressException`.
- External provider errors/timeouts -> currently surface as server failures without a dedicated client-facing error contract.
- Partially malformed SDMX payload -> parser skips invalid entries and continues with valid observations.

## 7. Test Story
- Explain confidence in three layers:
  - **Web/API behavior**: endpoint status and payload shape checks in web MVC tests.
  - **Business logic**: service tests for validation, mapping, and conversion calculations.
  - **Sync/parser robustness**: sync behavior and parser edge cases are covered.
- Be transparent about gaps:
  - No full repository integration tests for query behavior in real DB conditions.
  - No outbound-client resilience tests (timeouts/retry/failure policies).
  - No concurrency stress test for simultaneous sync requests.

## 8. Closing
If I had more time, I would prioritize:
- Introduce structured error responses and explicit mappings for provider failures.
- Tighten sync completeness checks beyond date-only coverage.
- Add lightweight retry/backoff and operational metrics for sync/external calls.
- Add persistence integration tests to validate query correctness and paging behavior end-to-end.
