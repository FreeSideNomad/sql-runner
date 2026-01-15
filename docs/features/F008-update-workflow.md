# F008: UPDATE Workflow

## Description

Implement the 5-step UPDATE workflow wizard: Parameters → Preview → Confirm → Execute → Result. Includes backup creation, transactional execution, and manual rollback capability.

## Acceptance Criteria

- [ ] Step 1: Parameter input form
- [ ] Step 2: Preview affected records (execute selectSql)
- [ ] Step 3: Confirmation summary
- [ ] Step 4: Execute update with progress indicator
- [ ] Step 5: Result display with rollback option
- [ ] Maximum 100,000 rows enforced
- [ ] Backup created before update (JSON blob)
- [ ] Single transaction for update
- [ ] Rollback restores only rollbackColumns
- [ ] Rollback is one-time only (marked as used)
- [ ] All steps logged to audit trail

## User Stories

- [ ] F008-S001: Create update wizard UI framework (5 steps)
- [ ] F008-S002: Implement Step 1 - Parameter form
- [ ] F008-S003: Implement Step 2 - Preview with selectSql
- [ ] F008-S004: Implement Step 3 - Confirmation summary
- [ ] F008-S005: Implement Step 4 - Execute update
- [ ] F008-S006: Implement Step 5 - Result display
- [ ] F008-S007: Create backup service (JSON storage)
- [ ] F008-S008: Implement rollback generation
- [ ] F008-S009: Implement rollback execution
- [ ] F008-S010: Add max rows validation (100K limit)

## Technical Notes

- Wizard state managed via session or hidden fields
- Backup stored in `backup_records` table
- Rollback SQL pattern: individual UPDATE per row
- Transaction isolation: READ_COMMITTED

## Dependencies

- F006 (query templates)
- F007 (parameter forms, execution service base)
- F009 (execution logging)
