## Overview

Consolidate configuration parsing/validation so every consumer interprets query YAML the same way, rejects malformed data early, and prevents unsafe SnakeYAML usage.

## Motivation

- `QueryExecutionService.parseConfig` hand-rolls YAML parsing, so fields such as `updateBindingMode` or future schema updates can drift from the editor/exporters.
- Raw `new Yaml()` calls instantiate arbitrary classes and provide no schema validation, exposing the app to deserialization attacks through imports.
- Validation is scattered: UPDATE workflows get bespoke checks in controllers while SELECT configurations remain unverified.

## Scope

1. Replace ad-hoc parsing with `ConfigYamlService` (or a new shared component) for SELECT execution, imports, and previews.
2. Harden YAML parsing by using SnakeYAML’s SafeConstructor or equivalent.
3. Layer Bean Validation (JSR-380) over `QueryConfig`/form DTOs and surface consistent error messages.
4. Extend `QueryConfigValidator` so SELECT and UPDATE configs are verified uniformly (e.g., duplicate parameters, missing PK when rollback columns exist).

## Detailed Changes

- `ConfigYamlService`
  - Instantiate SnakeYAML with `SafeConstructor`.
  - Provide helper methods `parseStrict` that run validation and throw descriptive exceptions.
- `QueryExecutionService`
  - Inject `ConfigYamlService` instead of duplicating parse logic.
  - Remove duplicated parameter extraction code once DTOs are validated.
- `ConfigImportService` / `ConfigExportService`
  - Parse imported YAML with the safe parser.
  - Validate every query before persisting; fail entire import if any entry is invalid.
- `QueryController`
  - Replace controller-level mode checks with validator outputs; ensure `BindingResult` shows structured errors for both SELECT and UPDATE configs.

## Risks & Mitigations

- **Risk:** Tightened validation may reject legacy YAML.
  - *Mitigation:* Provide migration notes and feature flag a “permissive” mode during rollout.
- **Risk:** New parser could reformat YAML unexpectedly.
  - *Mitigation:* Continue using existing dumper settings and add regression tests covering round-trips.

## Testing Strategy

- Unit tests for `ConfigYamlService` verifying safe parsing and validation error messaging.
- Controller tests ensuring invalid configs surface field errors.
- Import integration test feeding malicious YAML payloads (e.g., custom tags) to confirm they are rejected.

## Open Questions

1. Should validation run synchronously on every keystroke in the UI, or only on save?
2. Is there appetite for versioned config schemas to ease future migrations?
