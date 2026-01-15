# F008-S011: Enhanced UPDATE Parameter Binding

## User Story

**As a** query author
**I want** UPDATE SQL to reference SELECT preview results as parameters, and be able to download the final SQL script
**So that** I can perform targeted batch updates or per-row transformations, and have the option to execute them manually using external tools with full transaction control.

## Problem Statement

Currently, UPDATE_WORKFLOW queries use the same user-input parameters for both SELECT (preview) and UPDATE execution. The SELECT preview shows affected rows, but the UPDATE cannot reference individual row values or the set of selected primary keys. Furthermore, there is no way to audit or manually execute the exact SQL that the system will run.

**Current limitation:**
```yaml
selectSql: SELECT id, name FROM customers WHERE region = :region
updateSql: UPDATE customers SET status = 'ACTIVE' WHERE region = :region
# UPDATE affects ALL customers in region, not just the previewed ones!
```

The user sees 50 rows in preview, but the UPDATE might affect 1000 rows if more data was added between preview and execution.

## Solution

Enable two new parameter binding modes that allow UPDATE SQL to reference SELECT preview results:

### Mode 1: Batch Update using `:id_list`

When the UPDATE SQL contains the special parameter `:id_list`, the system:
1. Collects all primary key values from the SELECT preview results
2. Binds them as a list parameter
3. Executes a single UPDATE with an IN clause

**Example Configuration:**
```yaml
updateBindingMode: BATCH
selectSql: |
  SELECT id, name, status
  FROM customers
  WHERE region = :region AND status = :oldStatus
updateSql: |
  UPDATE customers
  SET status = :newStatus, modified_date = GETDATE()
  WHERE id IN (:id_list)
primaryKeyColumn: id
backupColumns: [id, name, status, modified_date]
rollbackColumns: [status]
parameters:
  - name: region
    label: Region
    dataType: STRING
    required: true
  - name: oldStatus
    label: Current Status
    dataType: ENUM
    enumValues:
      - value: INACTIVE
      - value: PENDING
  - name: newStatus
    label: New Status
    dataType: STRING
    required: true
```

**Execution Flow:**
1. User enters: `region=WEST`, `oldStatus=INACTIVE`, `newStatus=ACTIVE`
2. SELECT preview returns 50 rows with IDs: [101, 102, 103, ...]
3. System builds `id_list = [101, 102, 103, ...]`
4. Executes: `UPDATE customers SET status = 'ACTIVE', modified_date = GETDATE() WHERE id IN (101, 102, 103, ...)`
5. Only the 50 previewed rows are updated

**Benefits:**
- Single database round-trip (efficient)
- Atomic operation
- Guarantees only previewed rows are affected

---

### Mode 2: Row-by-Row Update using Column Parameters

When the UPDATE SQL contains parameter names that match SELECT column names (e.g., `:id`, `:name`, `:region`), the system:
1. Identifies which parameters are column-bound vs user-bound
2. Executes one UPDATE per SELECT preview row
3. Binds each row's column values to the corresponding parameters

**Example Configuration:**
```yaml
updateBindingMode: ROW_BY_ROW
selectSql: |
  SELECT id, name, email, region
  FROM customers
  WHERE status = :status
updateSql: |
  UPDATE customers
  SET name = UPPER(:name),
      email = LOWER(:email),
      processed_at = GETDATE()
  WHERE id = :id
primaryKeyColumn: id
backupColumns: [id, name, email, processed_at]
rollbackColumns: [name, email]
parameters:
  - name: status
    label: Status Filter
    dataType: STRING
    required: true
```

**Execution Flow:**
1. User enters: `status=PENDING`
2. SELECT preview returns:
   | id | name | email | region |
   |----|------|-------|--------|
   | 1 | john doe | John@Example.COM | EAST |
   | 2 | jane smith | JANE@EXAMPLE.COM | WEST |
3. System detects `:id`, `:name`, `:email` are SELECT column names
4. Executes for each row:
   - `UPDATE customers SET name = 'JOHN DOE', email = 'john@example.com', processed_at = GETDATE() WHERE id = 1`
   - `UPDATE customers SET name = 'JANE SMITH', email = 'jane@example.com', processed_at = GETDATE() WHERE id = 2`

**Benefits:**
- Per-row transformations possible
- Can use multiple columns from SELECT results
- Flexible update patterns

---

### Mixed Parameters

Parameters are classified as:
- **User-bound**: Defined in `parameters` config, entered via form (e.g., `:newStatus`, `:region`)
- **Column-bound**: Match SELECT column names, bound from preview data (e.g., `:id`, `:name`)

Both can be used in the same UPDATE:

```yaml
updateBindingMode: ROW_BY_ROW
selectSql: SELECT id, name, region FROM customers WHERE status = :oldStatus
updateSql: |
  UPDATE customers
  SET status = :newStatus,    -- user parameter (from form)
      name = UPPER(:name)     -- column parameter (from each row)
  WHERE id = :id              -- column parameter (from each row)
    AND region = :region      -- could be user OR column (column takes precedence)
parameters:
  - name: oldStatus
    dataType: STRING
  - name: newStatus
    dataType: STRING
```

**Disambiguation Rule:** If a parameter name exists in both user config AND SELECT columns, the SELECT column value takes precedence for that row.

---

## Update Binding Mode Selection (UI)

The update binding mode is **explicitly selected** in the query edit form:

### UI Design

On the query create/edit form, when `queryType = UPDATE_WORKFLOW`:

```
Update Binding Mode: [Dropdown]
+-------------------------------------------------------------+
| * Standard - Use only user-input parameters                 |
| * Batch (:id_list) - Single UPDATE with all preview IDs     |
| * Row-by-Row - One UPDATE per preview row using columns     |
+-------------------------------------------------------------+
```

**Mode descriptions shown in UI:**
- **Standard**: UPDATE uses the same parameters as SELECT (current behavior)
- **Batch**: Collects all primary key values into `:id_list` for IN clause
- **Row-by-Row**: Executes UPDATE for each row, binding column values as parameters

### Validation on Edit Screen

The edit form validates that UPDATE SQL uses correct parameters **before saving**:

**For BATCH mode:**
- `updateSql` must contain `:id_list` parameter
- `primaryKeyColumn` must be defined
- Error if `:id_list` is missing: "Batch mode requires :id_list parameter in UPDATE SQL"

**For ROW_BY_ROW mode:**
- At least one parameter in `updateSql` must match a column from `selectSql`
- `primaryKeyColumn` should be used in WHERE clause (warning if missing)
- Error if no column params found: "Row-by-row mode requires column parameters (e.g., :id) in UPDATE SQL"

**For STANDARD mode:**
- All parameters in `updateSql` should be defined in `parameters` config
- Warning if `updateSql` contains undeclared parameters

### Parameter Availability Display

When editing UPDATE SQL, show available parameters:

```
+-------------------------------------------------------------+
| Available Parameters:                                        |
|                                                              |
| User Parameters (from form):                                 |
|   :region, :oldStatus, :newStatus                           |
|                                                              |
| Column Parameters (from SELECT - Row-by-Row mode):          |
|   :id, :name, :email, :status                               |
|                                                              |
| Special Parameters (Batch mode):                            |
|   :id_list (list of all primary key values)                 |
+-------------------------------------------------------------+
```

This helps query authors know which parameters they can use.

---

## Acceptance Criteria

### UI Mode Selection
- [ ] Query edit form shows Update Binding Mode dropdown for UPDATE_WORKFLOW queries
- [ ] Dropdown options: Standard, Batch (:id_list), Row-by-Row
- [ ] Mode selection is **required** (no default, must be explicitly chosen)
- [ ] Selected mode saved to `updateBindingMode` field in QueryConfig
- [ ] Mode displayed on query view page

### Edit Screen Validation
- [ ] BATCH mode: Error if `:id_list` not in updateSql
- [ ] BATCH mode: Error if `primaryKeyColumn` not defined
- [ ] ROW_BY_ROW mode: Error if no column parameters found in updateSql
- [ ] ROW_BY_ROW mode: Warning if `primaryKeyColumn` not in WHERE clause
- [ ] STANDARD mode: Warning if updateSql has undeclared parameters
- [ ] Available parameters shown based on SELECT columns and user params

### Parameter Availability Display
- [ ] Edit screen shows "User Parameters" from parameters config
- [ ] Edit screen shows "Column Parameters" parsed from selectSql
- [ ] Edit screen shows ":id_list" as special parameter for Batch mode
- [ ] Parameters update dynamically when selectSql is modified

### Batch Mode (`:id_list`)
- [ ] All `primaryKeyColumn` values from preview are collected into list
- [ ] Single UPDATE statement executed with IN clause
- [ ] Null primary key values are filtered out
- [ ] Empty preview data results in zero-row update (no error)
- [ ] User parameters (e.g., `:newStatus`) still work alongside `:id_list`

### Row-by-Row Mode (column parameters)
- [ ] One UPDATE executed per preview row
- [ ] Each row's column values bound to matching parameters
- [ ] User parameters combined with column parameters for each row
- [ ] All updates execute in single transaction
- [ ] Transaction rolls back if any row fails
- [ ] Progress logged every 100 rows for large datasets

### Backup & Rollback
- [ ] Backup created before UPDATE execution (all modes)
- [ ] Backup stores original SELECT results (primaryKeyColumn + backupColumns)
- [ ] Rollback iterates through backed up rows
- [ ] For each row: UPDATE using primaryKeyColumn WHERE clause, SET rollbackColumns to original values
- [ ] Rollback always row-by-row (regardless of original update mode)
- [ ] Binding mode stored in execution metadata for audit

### UI Feedback
- [ ] Preview step shows detected binding mode
- [ ] Batch mode shows: "Batch update: N rows will be updated in single operation"
- [ ] Row-by-row mode shows: "Row-by-row update: N individual updates will be executed"
- [ ] Preview step provides "Download Script" button for UPDATE SQL
- [ ] Result/Complete step provides "Download Rollback Script" button

### Script Generation
- [ ] Generated script includes platform-specific transaction control (e.g., `BEGIN TRY/CATCH` for SQL Server)
- [ ] Script is self-contained and executable in external tools (SSMS, psql, etc.)
- [ ] Script uses the same parameter binding logic as internal execution
- [ ] Script includes comments identifying the mode and row count

---

## Rollback Strategy

Rollback is **always row-by-row**, regardless of the original update binding mode. This ensures precise restoration of original values.

### How Rollback Works

1. **Backup Data Structure** (stored as JSON):
```json
{
  "rows": [
    {"id": 101, "status": "INACTIVE", "name": "John Doe"},
    {"id": 102, "status": "PENDING", "name": "Jane Smith"}
  ]
}
```

2. **Rollback Generation**: For each backed up row, generate an UPDATE statement:
```sql
UPDATE customers
SET status = :status, name = :name
WHERE id = :id
```

3. **Rollback Execution**: Iterate through backup rows and execute UPDATE for each:
```
Row 1: UPDATE customers SET status = 'INACTIVE', name = 'John Doe' WHERE id = 101
Row 2: UPDATE customers SET status = 'PENDING', name = 'Jane Smith' WHERE id = 102
```

### Rollback Configuration

```yaml
primaryKeyColumn: id           # Used in WHERE clause
backupColumns: [id, status, name, email]  # Columns saved to backup
rollbackColumns: [status, name]           # Columns restored on rollback
```

- `backupColumns` must include `primaryKeyColumn` and all `rollbackColumns`
- `rollbackColumns` defines which columns are SET during rollback
- Only `rollbackColumns` are restored (not all `backupColumns`)

### Rollback Code Flow

```java
public void executeRollback(String executionLogId, String executedBy) {
    BackupRecord backup = backupRepository.findByExecutionLogId(executionLogId);
    List<Map<String, Object>> rows = objectMapper.readValue(backup.getBackupData(), ...);

    String pkColumn = config.getPrimaryKeyColumn();
    List<String> rollbackCols = config.getRollbackColumns();

    // Generate rollback SQL: UPDATE table SET col1 = :col1, col2 = :col2 WHERE pk = :pk
    String rollbackSql = buildRollbackSql(tableName, pkColumn, rollbackCols);

    for (Map<String, Object> row : rows) {
        Map<String, Object> params = new HashMap<>();
        params.put(pkColumn, row.get(pkColumn));  // WHERE clause
        for (String col : rollbackCols) {
            params.put(col, row.get(col));  // SET values
        }
        jdbc.update(rollbackSql, params);
    }

    backup.setIsRolledBack(true);
    backup.setRolledBackAt(LocalDateTime.now());
    backup.setRolledBackBy(executedBy);
}
```

### Why Row-by-Row for Rollback?

Even if the original update used BATCH mode (`:id_list`), rollback must be row-by-row because:
1. Each row may have different original values for `rollbackColumns`
2. Batch UPDATE cannot set different values per row
3. Row-by-row ensures precise restoration

---

## Technical Notes

### Files to Create

**`src/main/java/com/ivamare/domain/UpdateBindingMode.java`**
```java
public enum UpdateBindingMode {
    BATCH,       // Uses :id_list parameter for IN clause
    ROW_BY_ROW,  // Executes one UPDATE per preview row
    STANDARD     // Current behavior - user parameters only
}
```

**`src/main/java/com/ivamare/service/UpdateParameterAnalyzer.java`**
- Extract named parameters from SQL
- Detect binding mode from updateSql and SELECT columns
- Separate parameters into column-bound and user-bound groups

**`src/main/java/com/ivamare/service/QueryConfigValidator.java`**
- Validate UPDATE SQL parameters based on selected binding mode
- Return errors and warnings for display in edit form

### Files to Modify

- `src/main/java/com/ivamare/dto/QueryConfig.java` - Add `updateBindingMode` field
- `src/main/java/com/ivamare/dto/QueryConfigFormDto.java` - Add mode field to form DTO
- `src/main/java/com/ivamare/service/UpdateWorkflowService.java` - Add mode-specific execution methods
- `src/main/resources/templates/queries/form.html` - Add mode dropdown and validation display

---

## Test Plan

### Unit Tests
- [ ] UpdateParameterAnalyzer extracts parameters correctly
- [ ] UpdateParameterAnalyzer detects binding modes correctly
- [ ] QueryConfigValidator validates BATCH mode requirements
- [ ] QueryConfigValidator validates ROW_BY_ROW mode requirements
- [ ] QueryConfigValidator validates STANDARD mode warnings

### Integration Tests
- [ ] Batch update creates correct id_list from preview
- [ ] Row-by-row update processes all rows with correct bindings
- [ ] Rollback restores original values correctly (both modes)
- [ ] Transaction rollback on failure in row-by-row mode

### Manual Tests
1. Create query, select BATCH mode without :id_list - verify error shown
2. Create query, select ROW_BY_ROW without column params - verify error shown
3. Create batch mode query with :id_list, execute, verify single UPDATE
4. Create row-by-row query, execute, verify per-row updates
5. Test rollback after each mode

---

## Configuration Examples

### Example 1: Customer Status Batch Update
```yaml
name: Activate Customers by Region
description: Batch activate customers in a region
queryType: UPDATE_WORKFLOW
updateBindingMode: BATCH
selectSql: |
  SELECT id, name, status, email
  FROM customers
  WHERE region = :region AND status = :oldStatus
updateSql: |
  UPDATE customers
  SET status = :newStatus,
      activated_at = GETDATE(),
      activated_by = :activatedBy
  WHERE id IN (:id_list)
primaryKeyColumn: id
backupColumns: [id, status, activated_at, activated_by]
rollbackColumns: [status, activated_at, activated_by]
parameters:
  - name: region
    label: Region
    dataType: ENUM
    required: true
    enumValues:
      - value: EAST
      - value: WEST
      - value: NORTH
      - value: SOUTH
  - name: oldStatus
    label: Current Status
    dataType: ENUM
    required: true
    enumValues:
      - value: INACTIVE
      - value: PENDING
  - name: newStatus
    label: New Status
    dataType: STRING
    required: true
    defaultValue: ACTIVE
  - name: activatedBy
    label: Activated By
    dataType: STRING
    required: true
```

### Example 2: Email Normalization (Row-by-Row)
```yaml
name: Normalize Customer Emails
description: Convert emails to lowercase for each customer
queryType: UPDATE_WORKFLOW
updateBindingMode: ROW_BY_ROW
selectSql: |
  SELECT id, name, email
  FROM customers
  WHERE email LIKE :emailPattern
updateSql: |
  UPDATE customers
  SET email = LOWER(:email),
      name = TRIM(:name),
      normalized_at = GETDATE()
  WHERE id = :id
primaryKeyColumn: id
backupColumns: [id, name, email, normalized_at]
rollbackColumns: [email, name]
parameters:
  - name: emailPattern
    label: Email Pattern
    dataType: STRING
    required: true
    defaultValue: "%@%"
```

---

## Notes

- **Mode Required**: `updateBindingMode` is required for UPDATE_WORKFLOW queries
- **No Database Changes**: No schema modifications required

---

## Parent Feature

Relates to F008-update-workflow
