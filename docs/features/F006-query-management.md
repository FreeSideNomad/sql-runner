# F006: Query Template Management

## Description

Implement the admin interface for creating, editing, and versioning query templates. Includes YAML configuration editor with CodeMirror, category management (autocomplete tags), and version history.

## Acceptance Criteria

- [ ] Query list page grouped by category
- [ ] Create new query form with metadata fields
- [ ] CodeMirror YAML editor for query configuration
- [ ] Category autocomplete from existing categories
- [ ] Query versioning (each save creates new version)
- [ ] Version history view
- [ ] Query detail/edit page
- [ ] Query type selection (SELECT or UPDATE_WORKFLOW)
- [ ] Parameter definition in YAML
- [ ] Prod mode disables editing (read-only)

## User Stories

- [ ] F006-S001: Create Query entity and repository
- [ ] F006-S002: Create QueryVersion entity and repository
- [ ] F006-S003: Implement query service (CRUD + versioning)
- [ ] F006-S004: Build query list page with category grouping
- [ ] F006-S005: Integrate CodeMirror YAML editor
- [ ] F006-S006: Build query create/edit form
- [ ] F006-S007: Implement category autocomplete
- [ ] F006-S008: Build version history component
- [ ] F006-S009: Add YAML validation for query config
- [ ] F006-S010: Implement prod mode read-only behavior

## Technical Notes

- CodeMirror 6 for YAML editing
- YAML parsed with SnakeYAML
- Categories stored as string on query (no separate table)
- Self-contained YAML config per version

## Dependencies

- F004 (UI layout)
- F005 (database connections for dropdown)
