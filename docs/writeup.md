# Building a Production-Grade URL Shortener in Spring Boot 3 (and the Two Bugs I Only Caught by Running It)

Short links are a deceptively simple problem — `POST` a long URL, get back a short code, follow the code, get redirected. But "make it work" and "make it hold up in production" are very different bars. I built a URL shortener as a compact showcase of how I approach backend services, and the interesting part wasn't the happy path — it was the two bugs that only surfaced when I actually ran the thing end to end.

> **Code:** https://github.com/AfranUsmani/url-shortener-api

## The design in one breath

- **Java 21 · Spring Boot 3.3**, layered as controller → service → repository.
- **Short codes are Base62 encodings of the database id.** No random generation, no collision checks, no coordination — id `1 → "1"`, `62 → "10"`. Unique by construction.
- **Cache-aside reads.** The redirect path (`GET /{code}`) is the hot path, so resolutions are cached (Redis in production, in-memory locally) and only miss through to the database.
- **Atomic hit counting** via a single `UPDATE ... SET hit_count = hit_count + 1`, so concurrent redirects don't race.
- **Observable by default** — Spring Boot Actuator health checks and a `/actuator/prometheus` scrape endpoint via Micrometer.
- **OpenAPI/Swagger UI**, a consistent JSON error contract, Docker + Docker Compose, and GitHub Actions CI.

It runs with **zero infrastructure locally** (in-memory H2 + in-memory cache) and has a production-like Docker Compose path (PostgreSQL + Redis).

## Bug #1: the short code that was never there

My first cut of `create()` did the obvious thing:

1. Save the row to get its generated id.
2. Encode the id into a short code.
3. Save again.

With `GenerationType.IDENTITY`, that blew up: `NULL not allowed for column "SHORT_CODE"`. IDENTITY ids force an immediate `INSERT` to obtain the id — and that first insert goes in with a null short code, violating the `NOT NULL` constraint before step 2 ever runs.

Switching to a `SEQUENCE` id got me the id *before* the insert, but Hibernate snapshots the entity's state at `persist()` time — so the short code I set afterwards still wasn't in the queued insert.

The clean fix: derive the short code inside a **`@PrePersist` lifecycle callback**. By the time it fires, the sequence id is assigned, so a single `INSERT` carries a non-null short code. One write, no race, no null.

## Bug #2: the Prometheus endpoint that "worked" but 404'd in tests

My integration test hit `/actuator/prometheus` and got a `404`. But when I ran the packaged app and curled the same endpoint, it returned `200` with real metrics. Same code, opposite result.

The cause: **Spring Boot disables metrics export inside `@SpringBootTest` by default** so tests don't spin up exporters. The scrape endpoint simply isn't registered in the test context. The fix was `@AutoConfigureObservability`, plus moving the integration test to a real embedded server (`RANDOM_PORT` + `TestRestTemplate`) instead of `MockMvc` — a more faithful test anyway, since it exercises the actual HTTP stack.

## Why this matters

Both bugs were invisible to unit tests with mocks. They only appeared when the code met a real database and a real server. That's the whole point of the test pyramid having an integration layer — and the reason I don't consider generated or hand-written code "done" until I've watched it run. The final suite is 19 tests: fast unit tests for the encoder and service logic, and end-to-end integration tests against a live server covering create → redirect → stats, validation, error handling, the dashboard served at the root path, and the Prometheus endpoint.

## Takeaways

- **Encode identity, don't invent it.** Deriving short codes from a monotonic id removes an entire class of collision-handling code.
- **Know your ORM's lifecycle.** *When* a value is set relative to `persist()`/flush is as important as *what* it is.
- **Test the environment, not just the logic.** Mocks verify your intent; a real server verifies reality.
- **Run it.** The bugs that embarrass you in production are the ones that never showed up in a green mock-based build.

*Built with Java 21, Spring Boot 3, Redis, Prometheus, and Docker. Feedback and PRs welcome.*
