# AI use in this project.

Short description of how AI was used in this project.


## Summary

AI was used as a pair-programming assistant throughout this project.
- Primarily used Browser based GPT for desgin discussons and code snippets.
- Discussed the high-level design and break implementation into detailed, manageable steps.
- Larger feature development plan were decomposed into small, single-responsibility components.
- Code snippets were drafted with AI support. Then manually reviewed, refactored and improved.
- Codex agent used review implemented components and then manually refined.
- Components were are then inter-connected with AI support to resolve integration issues.
- This workflow reduced review complexity and helped avoid regressions.


## Details

GPT 5.2 browser based model and the Codex GPT 5.3 model were used in this project. Overall, the experience has been positive. For the most part, it can generate working code, depending on the complexity of the task and the limits of its training.

AI was not used to solve larger tasks in one shot. There are plenty of drawbacks in that appoach. Without a clear plan, the generated code can become unnecessarily complex. If asked to redo the same task, it can produce a completely different implementation, which creates a lot of review overhead. It's knowledge of the latest library versions can also be imperfect, so it may use outdated syntax. This one-shot flow also consumes significant tokens and time.

What worked best was using AI to discuss the high-level approach first, then creating a detailed implementation plan broken into smaller components. From there, AI supported on each component and then on integration with the wider system. This led to a faster iteration cycle, lower review cognitive load, and better oversight, since manual improvements, refactoring and adjustments were still necessary in most cases. Codex AI was very useful for reviewing ongoing progress against the initial plan and helping prioritize upcoming tasks.

AI was also a great tool for learning new ideas and concepts. For this purpose browser based model was preferrable since it is more verbose and can describe varietly of diifferent approaches which facilitate learning.

My thinking is to use AI like a pair-programming buddy and guide it through the implementation.

## Example usages

1. Finding Bundesbank API

Prompt:

Where can I find the official Bundesbank API for daily exchange rates? Show an example request and response format.

AI summarized the available endpoints and showed example

2. Refining the REST API

Prompt:

Refactor this Spring Boot endpoint to support pagination and enforce a maximum limit of 5000 results.

AI suggested adding limit validation and a pagination abstraction. I adopted the limit validation and 
the response structure.

3. Designing the Database Model

Prompt:

What do you think of this design for a database table to store daily exchange rates per currency with efficient lookups by date and currency?

AI suggested using a composite primary key (date, currency) instead of a surrogate ID. I accepted this because it reflects the natural uniqueness constraint and simplifies queries.

4. High-Level Design Discussion

Prompt:

Help me design the high-level architecture for a service that syncs exchange rates and exposes them via a REST API.

AI outlined a service–repository–controller structure and suggested clear separation of concerns. I used this as a starting point and refined it to keep the implementation simple.

5. Building Components per User Story

Prompt:

Generate a Spring service class that fetches exchange rates from an external API and stores them in a repository.

AI provided a code skeleton with dependency injection and error handling. I refactored it for simplicity and adjusted method signatures to match my data model.

6. Refining Sync Logic

Prompt:

Suggest a strategy to sync exchange rates only if local data is stale.

AI proposed scheduling, retries, and metadata tracking. I simplified the approach to a basic “sync last N days if stale” check.

7. Generating Unit Tests

Prompt:

Generate JUnit tests for this CurrencyService using Mockito, focus on core logic.

AI generated test scaffolding and mock setup. I refined test names, removed redundant cases, refined code accorinding to code changes.
