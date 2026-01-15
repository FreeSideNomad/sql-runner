## Overview

Make UPDATE workflows resilient by ensuring preview/update/rollback operations run atomically across external datasources, avoid session bloat, and generate dependable rollback SQL.

## Motivation

- Spring transactions do not cover the external JDBC pools provided by `ConnectionRegistry`, so failures can leave partial updates or orphaned backups.
- Preview data is stored in HTTP sessions (`List<Map<String,Object>>`), which is brittle in clustered deployments and can exceed memory.
- Rollback currently reverse-engineers the table name from raw SQL strings, failing on complex statements or alternate casing, and doesn’t guarantee backup columns cover rollback needs.

## Scope

1. Introduce explicit transaction boundaries for UPDATE workflows, even when using connection-registry datasources.
2. Replace session-based preview storage with server-side tokens (in DB or cache) referencing serialized previews.
3. Persist explicit table/schema/rollback SQL metadata instead of parsing the original `updateSql`.
4. Enforce configurable preview/update limits (`sqlrunner.update.max-affected-rows`) and surface warnings earlier.

## Detailed Changes

- Transaction handling
  - Add a service wrapper that borrows a `Connection` from the registry, disables auto-commit, and runs preview/update/rollback logic inside try-with-resources, committing only when all steps succeed.
  - Ensure backup inserts/log writes happen through the same logical transaction or document compensating rollback steps.
- Preview storage
  - Generate a short-lived preview token stored in a dedicated table (e.g., `update_previews`) with columns: token, query_id, params, preview_data (JSON), expires_at.
  - Controllers pass the token between steps; executing the update fetches and deletes the record.
- Rollback metadata
  - Extend query config to capture `targetTable` (schema-qualified) and optional `rollbackSqlTemplate`.
  - Update backup serialization to store primary key values plus all `rollbackColumns` case-insensitively.
  - Use stored metadata to generate deterministic rollback statements without regex parsing.
- Limits & telemetry
  - Read `sqlrunner.update.max-affected-rows` and block execution when preview rows exceed the limit unless an override flag is supplied.
  - Log audit events containing binding mode, row counts, and limit decisions.

## Risks & Mitigations

- **Risk:** Managing transactions manually may conflict with existing Spring-managed repositories.
  - *Mitigation:* Keep repository calls (backup/log) on the primary datasource while external updates run in their own DB transaction; document sequencing carefully.
- **Risk:** Preview tokens stored in DB introduce cleanup requirements.
  - *Mitigation:* Add scheduled job to purge expired tokens and enforce TTL at query time.

## Testing Strategy

- Integration tests executing STANDARD/BATCH/ROW_BY_ROW modes against the embedded H2 DB, verifying rollback restores original rows even when casing differs.
- Tests for preview token lifecycle (create, reuse, expiration).
- Failure-mode tests: simulate exception during update to confirm the external DB transaction rolls back and backup/log entries don’t persist inconsistent data.

## Open Questions

1. Should preview data be stored hashed/encrypted at rest?
2. Do we need role-based approvals when row counts exceed a threshold before permitting execution?
