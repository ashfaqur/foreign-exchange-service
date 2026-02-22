# Code Review and Objective Compliance

## Scope
This review evaluates the current implementation against the original Crewmeister user stories and submission expectations.

## Findings (By Severity)

1. High: Required `AI_USAGE.md` is missing  
- The challenge explicitly requires an `AI_USAGE.md` file.  
- This is a submission compliance blocker.

2. High: Sync completeness check can falsely treat data as complete  
- File: `service/src/main/java/com/crewmeister/cmcodingchallenge/sync/SyncService.java`  
- Current check uses `countDistinctDates(start, end)` only.  
- If a date exists with only some currencies, sync may be skipped even though FX data is incomplete.

3. Medium: Story 3 invalid-date error message does not match expected contract  
- File: `service/src/main/java/com/crewmeister/cmcodingchallenge/exception/ApiExceptionHandler.java`  
- Current message: `"Invalid input"` for type mismatch.  
- Expected by story: `400 -> "Invalid date format"`.

4. Medium: Story 2 response metadata does not represent effective range when filters are omitted  
- File: `service/src/main/java/com/crewmeister/cmcodingchallenge/currency/CurrencyService.java`  
- `start/end` in response currently echo request values; when omitted they remain `null`.  
- This differs from the documented sample behavior showing range values.

5. Medium: Missing story-level automated tests  
- Existing tests focus mostly on parser behavior.  
- There are no endpoint/service tests that assert user-story contracts for:
  - `/api/rates` pagination + validation + ordering
  - `/api/rates/{date}` with 404 behavior
  - `/api/conversions/to-eur` validation and conversion outputs
  - expected `400/404` payload semantics

6. Low: Live external smoke test is brittle  
- File: `service/src/test/java/com/crewmeister/cmcodingchallenge/BankRestClientSmokeTest.java`  
- Uses real network/API and can fail due external instability, reducing repeatability.

## User Story Compliance Snapshot

1. As a client, I want to get a list of all available currencies  
- Implemented: `GET /api/currencies`  
- Status: Implemented

2. As a client, I want to get all EUR-FX exchange rates at all available dates as a collection  
- Implemented: `GET /api/rates` with optional filters and pagination  
- True offset paging implemented via repository custom query (`setFirstResult`, `setMaxResults`)  
- Status: Implemented (metadata behavior caveat noted above)

3. As a client, I want to get the EUR-FX exchange rate at particular day  
- Implemented: `GET /api/rates/{date}` with optional `currency` and not-found handling  
- Status: Implemented (invalid-date message mismatch noted above)

4. As a client, I want to get a foreign exchange amount for a given currency converted to EUR on a particular day  
- Implemented: `GET /api/conversions/to-eur?date&currency&amount`  
- Status: Implemented

## What Remains Before Submission

1. Add `AI_USAGE.md` at repository root with required details:
- AI tools used
- key prompts/questions
- important AI responses
- acceptance/rejection reasoning

2. Strengthen sync completeness logic:
- Replace date-only coverage check with a strategy that verifies data completeness per date/currency in range.

3. Align story-3 error message contract:
- Return `"Invalid date format"` for date parse/type mismatch.

4. Add targeted story-level tests:
- Controller/service tests for stories 2–4, including negative scenarios and status-code semantics.

5. Improve documentation for submission:
- Update `README.md` with endpoint list, sample requests, and expected responses.
- Include local run and Docker run guidance.
- Include Swagger URL if enabled.

6. Consider isolating live smoke test from default test suite:
- Keep deterministic tests as default, and run network smoke tests optionally.

