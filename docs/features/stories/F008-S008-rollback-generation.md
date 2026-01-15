# F008-S008: Implement Rollback Generation

## User Story

**As a** developer
**I want** to generate rollback SQL
**So that** original values can be restored

## Acceptance Criteria

- [ ] Given backup data, then rollback SQL generated
- [ ] Given rollback SQL, then one UPDATE per row
- [ ] Given rollback SQL, then only rollbackColumns restored
- [ ] Given ID column, then used in WHERE clause
- [ ] Given SQL, then parameters bound safely

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/RollbackService.java`

### Rollback SQL Pattern
```sql
-- For each row in backup:
UPDATE table_name SET
  column1 = :value1,
  column2 = :value2
WHERE id = :id
```

### Rollback Service
```java
@Service
@RequiredArgsConstructor
public class RollbackService {
    private final BackupService backupService;
    private final QueryService queryService;

    public List<RollbackStatement> generateRollbackStatements(String backupId, String queryId) {
        BackupData backup = backupService.getBackupData(backupId);
        QueryConfig config = queryService.getCurrentConfig(queryId);

        List<String> rollbackColumns = config.getRollbackColumns();
        String idColumn = findIdColumn(backup.getColumns(), config);

        int idIndex = backup.getColumns().indexOf(idColumn);

        return backup.getRows().stream()
            .map(row -> generateStatement(config.getTableName(), rollbackColumns,
                                         backup.getColumns(), row, idColumn, idIndex))
            .toList();
    }

    private RollbackStatement generateStatement(String table, List<String> rollbackCols,
                                                List<String> allCols, List<Object> row,
                                                String idCol, int idIndex) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(table).append(" SET ");

        Map<String, Object> params = new HashMap<>();
        List<String> setClauses = new ArrayList<>();

        for (String col : rollbackCols) {
            int colIndex = allCols.indexOf(col);
            if (colIndex >= 0) {
                setClauses.add(col + " = :v_" + col);
                params.put("v_" + col, row.get(colIndex));
            }
        }

        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE ").append(idCol).append(" = :id");
        params.put("id", row.get(idIndex));

        return new RollbackStatement(sql.toString(), params);
    }

    private String findIdColumn(List<String> columns, QueryConfig config) {
        // Use configured ID column or default to first column
        if (config.getIdColumn() != null) {
            return config.getIdColumn();
        }
        return columns.stream()
            .filter(c -> c.equalsIgnoreCase("id"))
            .findFirst()
            .orElse(columns.get(0));
    }
}

@Data
@AllArgsConstructor
public class RollbackStatement {
    private String sql;
    private Map<String, Object> parameters;
}
```

### YAML Config Addition
```yaml
selectSql: |
  SELECT id, name, status FROM customers WHERE region = :region
updateSql: |
  UPDATE customers SET status = :newStatus WHERE region = :region
rollbackColumns:
  - status
idColumn: id  # Optional, defaults to "id" or first column
tableName: customers
```

## Test Plan

- [ ] Unit test: Rollback SQL generated correctly
- [ ] Unit test: Only rollback columns included
- [ ] Unit test: ID column in WHERE clause

## Parent Feature

Relates to F008-update-workflow
