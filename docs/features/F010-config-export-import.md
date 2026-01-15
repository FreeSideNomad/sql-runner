# F010: Configuration Export/Import

## Description

Implement YAML-based configuration export and import for query templates. Enables environment promotion (Test → Prod) and backup/restore of query configurations.

## Acceptance Criteria

- [ ] Export all queries to single YAML file
- [ ] Export includes metadata + versioned config
- [ ] Import validates YAML structure
- [ ] Import conflict detection (ID exists with different content)
- [ ] New queries added automatically
- [ ] Updated queries require higher version number
- [ ] Prod mode allows import only (no direct edits)
- [ ] Export filename: `sqlrunner-export-{timestamp}.yaml`
- [ ] Admin-only access

## User Stories

- [ ] F010-S001: Create export service (YAML generation)
- [ ] F010-S002: Build export page with download
- [ ] F010-S003: Create import service (YAML parsing)
- [ ] F010-S004: Implement import validation
- [ ] F010-S005: Build import page with file upload
- [ ] F010-S006: Add conflict detection and reporting
- [ ] F010-S007: Implement prod mode restrictions

## Technical Notes

- Use SnakeYAML for serialization
- Export format version: "1.0"
- Credentials never included in export
- Connection names must match target environment

## Dependencies

- F006 (query management)
