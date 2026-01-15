## Overview

Improve long-term reliability by expanding automated coverage, emitting metrics, and cleaning up duplicated controller/template wiring.

## Motivation

- New UPDATE binding modes and rollback logic currently lack integration tests, risking regressions.
- Operators have no visibility into execution rates, active sessions, or rollback counts beyond log scraping.
- Controllers duplicate boilerplate (e.g., populating dropdown lists), increasing maintenance overhead and risk of inconsistent data.

## Scope

1. Add integration tests for UPDATE workflows (standard/batch/row-by-row) plus rollback, covering both service and controller layers.
2. Instrument key services with Micrometer metrics and expose them via `/actuator/metrics`.
3. Refactor repeated controller setup logic into reusable helpers/components.

## Detailed Changes

- Testing
  - Build parametrized tests around `UpdateWorkflowService` using the embedded H2 DB to validate binding modes, limits, and rollback results.
  - Add controller tests ensuring UI flows (preview → execute → complete → rollback) function with the new preview tokens from E3.
  - Expand repository tests to cover filtering/pagination edge cases.
- Observability
  - In `QueryExecutionService`, record timers/counters for success/failure/timeout, gauge active executions, and include binding mode tags.
  - Expose additional health indicators (e.g., connection pool status via `ConnectionRegistry`).
  - Optionally integrate structured logging (JSON) for execution events.
- Maintenance
  - Extract a `QueryFormModelBuilder` (or similar) consumed by both `new` and `edit` endpoints to avoid repeated attribute wiring.
  - Centralize CSV escaping/writing utilities to prevent divergence between controllers.

## Risks & Mitigations

- **Risk:** Additional tests may lengthen CI time.
  - *Mitigation:* Use Testcontainers to parallelize DB-dependent suites and leverage Maven’s `failsafe` configuration for integration tests.
- **Risk:** Instrumentation could add slight overhead.
  - *Mitigation:* Use lightweight Micrometer timers and sample rates appropriate for expected load.

## Testing Strategy

- CI should run unit + integration suites with coverage reports (still enforcing 80% minimum via JaCoCo profile).
- Smoke-test metrics exposure by hitting `/actuator/metrics/sqlrunner.executions` in integration tests.

## Open Questions

1. Should we introduce snapshot tests for the Thymeleaf templates to catch accidental layout changes?
2. Is there value in providing a CLI harness or scripted smoke test for admins to verify UPDATE workflows before production changes?
