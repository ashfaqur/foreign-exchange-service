# REVIEW

## 1. Executive Summary
This submission is a solid, working Spring Boot solution for the coding challenge and is close to production-friendly in structure, but not yet production-ready in resilience and API consistency. The architecture is clear, requirements are largely covered, and there is meaningful unit/web-layer test coverage. The main gaps are around operational hardening (retry/circuit breaker/metrics), consistent error contracts, and a few correctness edge cases in sync and validation logic.

### Strengths
- Clear separation of concerns across `CurrencyController`, business services (`CurrencyService`, `CurrencyConversionService`), sync orchestration (`SyncService`), external integration (`BankService`, `BankRestClient`, `BankResponseParser`), and persistence (`ExchangeRateRepository`).
- Input validation and guardrails are present for key query operations (date windows, pagination bounds, currency format, positive amounts), with useful HTTP mappings in `ApiExceptionHandler`.
- Test suite covers core behavior in parser logic, service logic, sync behavior, and web endpoints via dedicated test classes.

### Weaknesses
- External integration resilience is limited: timeouts are configured, but there is no retry/backoff/circuit breaker strategy, and external failures are not consistently mapped to stable API responses.
- Error payloads are plain strings and vary by failure path, which makes client integration harder than a structured error schema.
- Sync correctness assumptions are optimistic (date-level coverage check assumes all currencies are present), and default date-window handling is inconsistent between components.

## 2. Functional Requirements Coverage

### List available currencies
- Capability: `GET /api/currencies`
- Implemented through: `CurrencyController#getCurrencies` -> `CurrencyService#getCurrencies` -> `SyncService#syncLastDays` -> `ExchangeRateRepository#findDistinctCurrencies`
- Coverage assessment: Implemented and functional.
- Gap: If sync fails due external/provider issues, behavior depends on available local data and may surface generic server errors.

### Get EUR-FX rates collection
- Capability: `GET /api/rates` with optional `start`, `end`, `currency`, `limit`, `offset`
- Implemented through: `CurrencyController#getRates` -> `CurrencyService#getRates(...)` -> `SyncService#syncRange(...)` -> repository count + paged fetch
- Coverage assessment: Implemented with date-range filtering, optional currency filter, and offset/limit pagination.
- Gaps:
  - Max range guard exists (90 days), but offset pagination can degrade for larger result sets.
  - Error shape remains plain text instead of a typed response contract.

### Get rates for a specific day
- Capability: `GET /api/rates/{date}` with optional `currency`
- Implemented through: `CurrencyController#getRatesByDate` -> `CurrencyService#getRatesByDate` -> `SyncService#syncDay` -> `ExchangeRateRepository#findByDateAndOptionalCurrency`
- Coverage assessment: Implemented and returns a date-keyed rate map.
- Gap: Currency format is normalized but not strictly validated in this path, so malformed values can fall through to not-found behavior.

### Convert foreign amount to EUR
- Capability: `GET /api/conversions/to-eur?date&currency&amount`
- Implemented through: `CurrencyController#convertToEur` -> `CurrencyConversionService#convertToEur`
- Coverage assessment: Implemented with validation, EUR special case, and deterministic rounding.
- Gap: Rounding policy is fixed and implicit; no explicit API-level statement of precision strategy beyond behavior.

### Trigger manual update/sync
- Capability: `POST /api/update?start&end`
- Implemented through: `CurrencyController#update` -> `CurrencyService#forceUpdateData` -> `SyncService#syncRange(..., true)`
- Coverage assessment: Implemented and returns `204` on success.
- Gap: No auth/rate limiting around a side-effect endpoint; in a non-challenge environment this is high risk.

## 3. Architecture & Code Organization

### Component breakdown
- Controller layer: `CurrencyController` exposes all REST endpoints and delegates behavior.
- Service layer: `CurrencyService` handles query use cases and range logic; `CurrencyConversionService` handles conversion-specific validation and arithmetic.
- Sync layer: `SyncService` orchestrates fetch-or-skip logic and enforces single-sync concurrency via `ReentrantLock`; `DbWriter` wraps transactional writes.
- External client layer: `BankService` composes `BankRestClient` and `BankResponseParser`.
- Persistence layer: JPA entity + embedded ID + repository + custom query implementation.
- DTO layer: record-based response DTOs for rates, conversion, and pagination metadata.

### Boundaries and coupling
- Good boundaries: transport concerns are mostly in controller/advice; business logic sits in services; outbound call/parsing are isolated.
- Notable coupling:
  - Read endpoints are tightly coupled to sync behavior (most read operations can trigger sync), which can impact latency and failure characteristics.
  - Sync coverage check depends on a date-level assumption rather than currency-level completeness.
  - Error behavior from external-client exceptions is not fully normalized by a dedicated domain-level boundary.

## 4. Data Model & Persistence

### Entity and ID design
- `ExchangeRateEntity` uses `ExchangeRateId` (`date`, `currency`) as a composite key.
- This model aligns with one rate per day/currency and naturally supports idempotent upsert-like saves through key reuse.

### Query and pagination approach
- Repository supports:
  - Distinct currency listing
  - Count by optional filters
  - Date/currency reads
  - Custom filtered/paged rate listing ordered by `date`, `currency`
- Pagination is offset-based with total count metadata.

### Correctness and performance considerations
- Correctness: date-range and pagination bounds are validated in `CurrencyService`.
- Performance: for larger datasets, offset pagination and per-request total count may become expensive.
- Data completeness risk: sync “covered” check counts distinct dates only, so partial day data can appear complete.

## 5. External Integration & Resilience

### Outbound call behavior
- `BankRestClient` uses Spring `RestClient` with configured connect/read timeouts.
- 4xx and 5xx responses are wrapped into custom runtime exceptions.

### Resilience posture
- Present: timeout controls and basic status classification.
- Missing: retry/backoff, circuit breaker, fallback policy, and provider health state tracking.

### Parsing robustness and fallback behavior
- `BankResponseParser` is defensive: it skips malformed series/observations and logs high skip ratios.
- This is robust against partial data corruption but can mask upstream schema drift because many invalid rows can be silently dropped.

## 6. Error Handling & API Semantics
- `ApiExceptionHandler` maps key exceptions to `400`, `404`, and `503`.
- Validation and not-found semantics are generally sensible for challenge requirements.
- Main consistency gap: error responses are plain text strings, not structured JSON with stable fields (`code`, `message`, `details`, `timestamp`, etc.).
- External-client exceptions are not explicitly mapped in the advice, so provider failures may surface as generic server errors.

## 7. Testing

### Existing coverage
- Web-layer endpoint behavior is covered in `CurrencyControllerWebMvcTest`.
- Query/conversion domain behavior and validation paths are covered in `CurrencyServiceTest` and `CurrencyConversionServiceTest`.
- Sync orchestration and force/coverage logic are covered in `SyncServiceTest`.
- SDMX parsing edge cases are covered in `BankResponseParserTest`.

### Missing or thin areas
- No true persistence integration tests validating JPA mappings/queries against H2 behavior.
- No tests for outbound client behavior under timeout/error conditions.
- No explicit concurrency test proving lock behavior under parallel sync attempts.
- No contract test for consistent structured error responses (currently not implemented).

## 8. Observability & Operability
- Logging exists in sync and parser flows and includes useful operational events.
- Actuator endpoints expose `health` and `info`.
- Startup sync is attempted via `StartupSyncConfig`, and failures are logged without preventing startup.
- Gaps:
  - No custom metrics for sync duration, rows ingested, parser skips, or provider failure rates.
  - Limited operational controls for degraded external-provider scenarios.

## 9. Security & Input Validation
- Positive:
  - Important inputs are validated in service methods (date ordering, range size, pagination bounds, amount positivity, currency format in key paths).
- Risks/gaps:
  - No authentication/authorization for update trigger endpoint.
  - H2 console is enabled, which is acceptable for challenge context but unsafe for production exposure.
  - Validation is not uniformly enforced across all endpoint branches (e.g., optional currency path differences).

## 10. Trade-offs & Improvement Plan

### P0 (Correctness and reliability first)
- Add explicit exception mapping for external provider failures (`BundesbankClientException`, `BundesbankServerException`) to stable API error responses.
- Fix sync coverage logic to detect partial-day currency gaps (not only distinct date presence).
- Align default “last N days” behavior across `CurrencyService` and `SyncService` so inclusive ranges are consistent.

### P1 (API quality and maintainability)
- Replace plain-text error responses with a structured error DTO and use it consistently across all handlers.
- Move request validation to declarative constraints where possible and harmonize optional currency validation behavior.
- Externalize provider URL/timeouts and sync tuning parameters into configuration properties for environment-level control.

### P2 (Operational hardening and scale)
- Add lightweight retry with backoff for transient provider failures and a simple circuit-breaker policy.
- Add metrics for sync outcomes, parser skip ratios, external latency, and error rates.
- Consider keyset/cursor pagination if dataset size or access volume grows beyond challenge scale.
