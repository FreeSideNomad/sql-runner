## Overview

Strengthen the security posture around connection management and configuration import/export flows.

## Motivation

- Missing environment variables currently result in blank usernames/passwords, leading to silent misconfigurations or unintended anonymous access.
- `ConfigImportService` and other YAML entry points use unsafe SnakeYAML constructors, making deserialization attacks possible.
- Connection tests and other admin endpoints do not sanitize user input or enforce strict auditing.

## Scope

1. Enforce strict credential resolution with clear errors, optional secret managers, and zero tolerance for blanks unless explicitly allowed.
2. Sanitize YAML imports/exports, reject unknown tags, and validate schema before persisting.
3. Audit admin operations (import/export/connection test) and ensure responses do not leak sensitive data.

## Detailed Changes

- `ConnectionRegistry`
  - Throw `IllegalStateException` when required credentials are missing; provide configuration to mark certain datasources as anonymous.
  - Support integration with environment abstraction (e.g., Spring Cloud Config or HashiCorp Vault) via a `CredentialProvider` interface.
  - In `testConnection`, wrap statement execution in try-with-resources and redact sensitive JDBC errors before logging.
- YAML Hardening
  - Reuse the safe parser defined in E1; forbid unknown properties using a schema whitelist.
  - Before executing imports, run a policy check (e.g., cannot import queries pointing to forbidden connections).
  - During export, scrub credential references and include metadata proving the export origin.
- Auditing & RBAC
  - Emit structured audit events (to logs and/or database) whenever an admin triggers import/export/test actions.
  - Ensure controllers surface permission errors consistently and that read-only mode blocks these endpoints even when accessed through REST.

## Risks & Mitigations

- **Risk:** Failing on missing credentials could break local dev setups.
  - *Mitigation:* Provide explicit fallbacks in `application-dev.yml` and document required env vars.
- **Risk:** Additional auditing may generate noisy logs.
  - *Mitigation:* Use dedicated logger categories so operators can filter as needed.

## Testing Strategy

- Unit tests for `CredentialProvider` resolution paths, including failure cases.
- Security tests verifying unsafe YAML payloads are rejected.
- Controller tests confirming read-only mode blocks import/export even when form submissions are spoofed.

## Open Questions

1. Should connection credentials be rotated automatically, and if so, how do we refresh Hikari pools safely?
2. Do we need to encrypt backups/import files at rest?
