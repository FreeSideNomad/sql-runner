# F008-S007: Create Backup Service (JSON Storage)

## User Story

**As a** developer
**I want** a backup service
**So that** original values can be stored before updates

## Acceptance Criteria

- [ ] Given preview data, then backup record created
- [ ] Given backup, then data stored as JSON
- [ ] Given backup, then column names preserved
- [ ] Given backup, then linked to execution log
- [ ] Given backup, then row count stored

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/BackupService.java`
- `src/main/java/com/ivamare/domain/BackupRecord.java` (from F002-S006)

### Backup Data Format
```json
{
  "columns": ["id", "name", "status", "modified_date"],
  "rows": [
    [1001, "Acme Corp", "A", "2024-01-15T10:30:00"],
    [1002, "Beta Inc", "A", "2024-01-15T11:45:00"]
  ]
}
```

### Backup Service
```java
@Service
@RequiredArgsConstructor
public class BackupService {
    private final BackupRecordRepository backupRepository;
    private final ObjectMapper objectMapper;

    public BackupRecord createBackup(String queryId, List<Map<String, Object>> previewData, String createdBy) {
        if (previewData.isEmpty()) {
            throw new IllegalArgumentException("Cannot create backup for empty data");
        }

        // Extract columns from first row
        List<String> columns = new ArrayList<>(previewData.get(0).keySet());

        // Convert to row arrays
        List<List<Object>> rows = previewData.stream()
            .map(row -> columns.stream()
                .map(row::get)
                .toList())
            .toList();

        BackupData backupData = new BackupData(columns, rows);

        BackupRecord record = BackupRecord.builder()
            .id(UUID.randomUUID().toString())
            .backupData(objectMapper.writeValueAsString(backupData))
            .rowCount(previewData.size())
            .isRolledBack(false)
            .createdAt(LocalDateTime.now())
            .build();

        return backupRepository.save(record);
    }

    public BackupData getBackupData(String backupId) {
        BackupRecord record = backupRepository.findById(backupId).orElseThrow();
        return objectMapper.readValue(record.getBackupData(), BackupData.class);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackupData {
    private List<String> columns;
    private List<List<Object>> rows;
}
```

### Backup Record Entity
```java
@Entity
@Table(name = "backup_records", schema = "sqlrunner")
@Data
@Builder
public class BackupRecord {
    @Id
    private String id;

    @Column(name = "execution_log_id")
    private String executionLogId;

    @Column(name = "backup_data", columnDefinition = "TEXT")
    private String backupData;

    @Column(name = "row_count")
    private int rowCount;

    @Column(name = "is_rolled_back")
    private boolean isRolledBack;

    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;

    @Column(name = "rolled_back_by")
    private String rolledBackBy;

    @Column(name = "rollback_execution_log_id")
    private String rollbackExecutionLogId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

## Test Plan

- [ ] Unit test: Backup data serialization
- [ ] Integration test: Backup record created
- [ ] Integration test: Backup data retrievable

## Parent Feature

Relates to F008-update-workflow
