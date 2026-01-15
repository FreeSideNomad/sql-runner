## Overview

Ensure SELECT execution respects configured safety limits, completes the cancellation workflow, and streams exports to avoid excessive memory usage.

## Motivation

- `timeoutSeconds`/`maxRows` exist in `QueryConfig` but are ignored, allowing runaway queries.
- The cancellation infrastructure tracks `activeExecutions` but never returns execution IDs to clients, so “Cancel” buttons are non-functional.
- CSV export endpoints materialize entire result sets in memory, risking OOM for wide tables.

## Scope

1. Honor query-specific and default timeouts plus row limits during SELECT execution.
2. Expose execution IDs to the UI/API, register JDBC statements, and implement cooperative cancellation.
3. Stream CSV exports row-by-row using JDBC cursor APIs or `RowCallbackHandler`.

## Detailed Changes

- `QueryExecutionService`
  - Inject default timeout/max-row settings (`sqlrunner.execution.*`).
  - Apply limits via `setQueryTimeout` / `setMaxRows` on statements.
  - Return a `ExecutionHandle` containing `executionId`, status, and result future.
  - Track `Statement` references and call `cancel()` when requested.
- Controllers/Views
  - Update execution endpoints plus CodeMirror overlay to capture the new execution ID.
  - Wire the cancel button to POST `/queries/{id}/execute/cancel/{executionId}` (new endpoint).
- CSV Export
  - Implement streaming writer: iterate with `NamedParameterJdbcTemplate.query(sql, params, rs -> { ... })`.
  - Optionally chunk commits or flush the servlet output every N rows.

## Risks & Mitigations

- **Risk:** Cancelling statements may leave transactions open on the target database.
  - *Mitigation:* Always execute queries in auto-commit mode for SELECTs, and log warnings when cancellation fails.
- **Risk:** Aggressive limits could surprise power users.
  - *Mitigation:* Provide per-query overrides (bounded by a global maximum) and display active limits in the UI.

## Testing Strategy

- Integration tests that configure low `timeoutSeconds` and verify that slow queries terminate with TIMEOUT status.
- Unit tests for the execution registry ensuring `cancelExecution` removes handles and calls `Statement#cancel`.
- Controller tests verifying the CSV endpoint streams large datasets without exhausting memory (use mocks to track flush count).

## Open Questions

1. Should execution handles be persisted (e.g., Redis) to support horizontal scaling?
2. How should we expose execution progress (rows fetched vs. total) to the UI?
