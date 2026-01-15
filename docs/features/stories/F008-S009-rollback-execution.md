# F008-S009: Implement Rollback Execution

## User Story

**As a** user
**I want** to execute a rollback
**So that** I can restore original values after an update

## Acceptance Criteria

- [ ] Given rollback request, then all statements executed
- [ ] Given rollback, then single transaction
- [ ] Given success, then backup marked as rolled back
- [ ] Given rollback, then logged as ROLLBACK execution type
- [ ] Given already rolled back, then request rejected
- [ ] Given failure, then transaction rolled back

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/service/RollbackService.java`
- `src/main/java/com/ivamare/controller/RollbackController.java`

### Rollback Execution
```java
@Service
@RequiredArgsConstructor
@Transactional
public class RollbackService {
    private final BackupRecordRepository backupRepository;
    private final ExecutionLogService logService;
    private final ConnectionRegistry connectionRegistry;

    public RollbackResult executeRollback(String backupId, String queryId, String executedBy) {
        BackupRecord backup = backupRepository.findById(backupId)
            .orElseThrow(() -> new EntityNotFoundException("Backup not found"));

        if (backup.isRolledBack()) {
            throw new IllegalStateException("This backup has already been rolled back");
        }

        Query query = queryService.getQueryById(queryId);
        List<RollbackStatement> statements = generateRollbackStatements(backupId, queryId);

        DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(ds);

        long startTime = System.currentTimeMillis();
        int totalRows = 0;

        try {
            for (RollbackStatement stmt : statements) {
                int affected = jdbc.update(stmt.getSql(), stmt.getParameters());
                totalRows += affected;
            }

            long duration = System.currentTimeMillis() - startTime;

            // Mark backup as rolled back
            backup.setRolledBack(true);
            backup.setRolledBackAt(LocalDateTime.now());
            backup.setRolledBackBy(executedBy);

            // Log rollback execution
            ExecutionLog log = logService.logExecution(
                query, Map.of(), totalRows, duration,
                ExecutionStatus.SUCCESS, null, ExecutionType.ROLLBACK
            );

            backup.setRollbackExecutionLogId(log.getId());
            backupRepository.save(backup);

            return RollbackResult.success(totalRows, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logService.logExecution(
                query, Map.of(), 0, duration,
                ExecutionStatus.FAILED, e.getMessage(), ExecutionType.ROLLBACK
            );
            throw new RollbackException("Rollback failed: " + e.getMessage(), e);
        }
    }
}
```

### Controller
```java
@Controller
@RequestMapping("/queries")
public class RollbackController {

    @PostMapping("/{backupId}/rollback")
    public String executeRollback(@PathVariable String backupId,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            BackupRecord backup = backupService.getBackup(backupId);
            RollbackResult result = rollbackService.executeRollback(
                backupId, backup.getQueryId(), auth.getName());

            redirectAttributes.addFlashAttribute("message",
                "Rollback completed: " + result.getRowCount() + " rows restored");

            return "redirect:/history/" + backup.getRollbackExecutionLogId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/history/" + backup.getExecutionLogId();
        }
    }
}
```

## Test Plan

- [ ] Integration test: Rollback executes all statements
- [ ] Integration test: Backup marked as rolled back
- [ ] Integration test: Second rollback rejected
- [ ] Integration test: Failure rolls back transaction

## Parent Feature

Relates to F008-update-workflow
