# F009-S006: Link Logs to Backup Records

## User Story

**As a** user
**I want** to see backup information from execution logs
**So that** I can understand what can be rolled back

## Acceptance Criteria

- [ ] Given UPDATE execution, then backup record linked
- [ ] Given log detail, then backup row count shown
- [ ] Given log detail, then backup creation time shown
- [ ] Given backed up log, then rollback status displayed
- [ ] Given rollback available, then rollback button shown

## Technical Notes

### Files to Create
- `src/main/resources/templates/history/backup-info.html`

### Backup Info Fragment
```html
<div th:fragment="backup(backup)">
    <div class="grid grid-cols-2 gap-4">
        <div>
            <span class="text-gray-600">Backup ID</span>
            <p class="font-mono text-sm" th:text="${backup.id}">uuid</p>
        </div>
        <div>
            <span class="text-gray-600">Rows Backed Up</span>
            <p th:text="${backup.rowCount}">0</p>
        </div>
        <div>
            <span class="text-gray-600">Created At</span>
            <p th:text="${#temporals.format(backup.createdAt, 'MMM d, yyyy HH:mm:ss')}">date</p>
        </div>
        <div>
            <span class="text-gray-600">Rollback Status</span>
            <p th:if="${backup.rolledBack}" class="text-purple-600">
                Rolled back on <span th:text="${#temporals.format(backup.rolledBackAt, 'MMM d HH:mm')}">date</span>
                by <span th:text="${backup.rolledBackBy}">user</span>
            </p>
            <p th:unless="${backup.rolledBack}" class="text-green-600">
                Available for rollback
            </p>
        </div>
    </div>

    <!-- Rollback Button -->
    <div th:if="${!backup.rolledBack}" class="mt-4 pt-4 border-t">
        <form th:action="@{/queries/{id}/rollback(id=${backup.id})}" method="post"
              onsubmit="return confirm('Are you sure you want to rollback these changes?');">
            <button type="submit" class="px-4 py-2 bg-yellow-600 text-white rounded hover:bg-yellow-700">
                Rollback Changes
            </button>
        </form>
    </div>

    <!-- View Rollback Log -->
    <div th:if="${backup.rolledBack && backup.rollbackExecutionLogId != null}" class="mt-4 pt-4 border-t">
        <a th:href="@{/history/{id}(id=${backup.rollbackExecutionLogId})}"
           class="text-rbc-blue hover:underline">
            View Rollback Execution Log →
        </a>
    </div>
</div>
```

### Linking in Execution Service
```java
// After successful update execution
ExecutionLog log = logService.logUpdateSuccess(query, params, rowCount, duration, backup.getId(), user);
backup.setExecutionLogId(log.getId());
backupRepository.save(backup);
```

### Bidirectional Navigation
```java
// From log to backup
BackupRecord backup = backupRepository.findByExecutionLogId(log.getId());

// From backup to logs
ExecutionLog updateLog = logRepository.findById(backup.getExecutionLogId());
ExecutionLog rollbackLog = logRepository.findById(backup.getRollbackExecutionLogId());
```

## Test Plan

- [ ] Integration test: Backup linked to log
- [ ] Integration test: Rollback status displayed
- [ ] Integration test: Rollback button triggers rollback

## Parent Feature

Relates to F009-execution-logging
