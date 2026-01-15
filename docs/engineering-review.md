1. **Configuration & Validation**
   - Consolidate YAML parsing through `ConfigYamlService`, enforce schema validation, and expose a single validator so both UI and runtime paths reject malformed configs early. See `docs/E1-configuration-and-validation-spec.md`.
2. **Query Execution Behavior**
   - Enforce max rows/timeouts, complete the cancellation story, and stream exports to keep SELECT execution predictable under load. See `docs/E2-query-execution-behavior-spec.md`.
3. **Update Workflow Reliability**
   - Make preview/update/rollback flows transactional across external datasources, avoid storing large previews in sessions, and harden backup/rollback derivation. See `docs/E3-update-workflow-reliability-spec.md`.
4. **Connection & Security Hardenings**
   - Fail fast when credentials are missing, prefer secure secret resolution, and sanitize YAML imports to prevent unsafe deserialization or privilege escalation. See `docs/E4-connection-and-security-hardenings-spec.md`.
5. **Testing, Observability & Maintenance**
   - Expand integration coverage for new binding modes, emit metrics around executions, and refactor duplicated controller wiring to keep the UI maintainable. See `docs/E5-testing-observability-and-maintenance-spec.md`.
