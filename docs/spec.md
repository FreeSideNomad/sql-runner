# SQL Runner - Technical Specification

## 1. Overview

SQL Runner is an enterprise web application for executing parameterized SQL statements against multiple database platforms with comprehensive audit logging, data backup, and rollback capabilities.

### 1.1 Key Features
- **Simple SELECT Execution**: Run parameterized SELECT queries with results display and CSV export
- **Complex UPDATE Workflow**: Multi-step wizard for safe data modifications with backup and rollback
- **Multi-Database Support**: SQL Server, DB2, PostgreSQL
- **Audit Logging**: Complete execution history with user, parameters, timing, and results
- **Configuration Management**: Version-controlled query templates with environment promotion
- **Role-Based Access Control**: AD group-based permissions (ADMIN, UPDATE_RUNNER, SELECT_RUNNER)

### 1.2 Target Users
- **Data Analysts**: Execute pre-approved SELECT queries for reporting
- **Data Stewards**: Perform controlled data modifications with audit trail
- **Administrators**: Manage query templates, categories, and system configuration

---

## 2. Functional Requirements

### 2.1 Authentication & Authorization

#### 2.1.1 Authentication
- **Method**: On-premises Active Directory via LDAP bind
- **Flow**: Form-based login (username/password validated against AD)
- **Session**: 20-minute timeout, no "remember me" option
- **Future**: Prepared for Redis-based session management

#### 2.1.2 Authorization (RBAC)
| Role | Permissions |
|------|-------------|
| ADMIN | Manage categories, queries, view all logs, export/import config |
| UPDATE_RUNNER | Execute SELECT and UPDATE workflows, view all logs |
| SELECT_RUNNER | Execute SELECT queries only, view all logs |

- Roles are mapped from AD groups (configurable mapping)
- All authenticated users can view execution history (no row-level security)

### 2.2 Query Template Management

#### 2.2.1 Query Structure
Each query template has metadata stored in the database and versioned configuration stored as YAML:

**Database fields (queries table):**
- **ID**: Unique identifier (UUID)
- **Name**: Human-readable display name
- **Description**: Purpose and usage notes
- **Category**: Simple tag for grouping (string, not FK)
- **Connection**: Named database connection
- **Type**: SELECT or UPDATE_WORKFLOW
- **Current Version**: Latest version number

**Versioned YAML configuration (query_versions table):**
- **SQL Content**: Parameterized SQL statement(s)
- **Parameters**: Ordered list of input parameters with types, labels, enums
- **Configuration**: Timeout, max rows overrides
- **UPDATE_WORKFLOW specific**: selectSql, updateSql, primaryKeyColumn, backupColumns, rollbackColumns

This design keeps each version self-contained and makes export/import straightforward.

#### 2.2.2 Parameter Definition
| Field | Description |
|-------|-------------|
| name | Placeholder name in SQL (e.g., `customerId`) |
| label | Display label in UI (e.g., "Customer ID") |
| dataType | STRING, INTEGER, DECIMAL, DATE, DATETIME, BOOLEAN, ENUM, LIST_STRING, LIST_INTEGER |
| required | Whether parameter is mandatory |
| defaultValue | Optional default value |
| validation | Optional regex pattern (STRING only) |
| enumValues | List of value/description pairs (ENUM only) |
| listSeparator | Separator for LIST types: `NEWLINE`, `COMMA`, or `BOTH` (default: `BOTH`) |

**Parameter Placeholder Syntax**: `:paramName` (Spring NamedParameterJdbcTemplate style)

**List Parameters**: LIST_STRING and LIST_INTEGER types allow users to enter multiple values in a multiline text box. Values can be separated by newlines, commas, or both (configurable). The parameter is passed to SQL as a collection for use with `IN` clauses.

Example SQL:
```sql
SELECT customer_id, name, email, status
FROM customers
WHERE region = :region
  AND status = :status
  AND created_date >= :startDate
```

Example SQL with list parameter:
```sql
SELECT customer_id, name, email, status
FROM customers
WHERE customer_id IN (:customerIds)
```

#### 2.2.3 Enum Parameters
- Defined per-query (not shared across queries)
- Each enum value has:
  - `value`: Actual value used in SQL (string or integer)
  - `description`: Display text in dropdown

Example:
```yaml
parameters:
  - name: status
    label: Customer Status
    dataType: ENUM
    enumValues:
      - value: "A"
        description: "Active"
      - value: "I"
        description: "Inactive"
      - value: "P"
        description: "Pending"
```

#### 2.2.4 Versioning
- Every edit creates a new version (immutable versions)
- Each version stores complete configuration as self-contained YAML
- Old versions are archived and viewable
- Execution logs reference the specific version executed
- Rollback operations use the version from original execution

#### 2.2.5 Categories
- Simple string tag on each query (no separate table)
- UI provides autocomplete from existing categories
- Users can type a new category name to create it
- Used for grouping/filtering queries on dashboard

### 2.3 Database Connections

#### 2.3.1 Connection Configuration
Connections are defined in application configuration (not in database):

```yaml
sqlrunner:
  connections:
    - name: "PROD_SQLSERVER"
      type: SQLSERVER
      displayName: "Production SQL Server"
      jdbcUrl: "jdbc:sqlserver://${PROD_SQL_HOST}:1433;databaseName=${PROD_SQL_DB}"
      credentialsEnvPrefix: "PROD_SQL"  # reads PROD_SQL_USER, PROD_SQL_PASSWORD
    - name: "PROD_DB2"
      type: DB2
      displayName: "Production DB2"
      jdbcUrl: "jdbc:db2://${PROD_DB2_HOST}:50000/${PROD_DB2_DB}"
      credentialsEnvPrefix: "PROD_DB2"
    - name: "PROD_POSTGRES"
      type: POSTGRESQL
      displayName: "Production PostgreSQL"
      jdbcUrl: "jdbc:postgresql://${PROD_PG_HOST}:5432/${PROD_PG_DB}"
      credentialsEnvPrefix: "PROD_PG"
```

#### 2.3.2 Credentials
- Database credentials are **never** stored in database or YAML exports
- Retrieved from environment variables at runtime
- Pattern: `{PREFIX}_USER`, `{PREFIX}_PASSWORD`

#### 2.3.3 Connection Test
- Admin UI provides "Test Connection" button
- Executes simple validation query (e.g., `SELECT 1`)
- Reports success or detailed error message

### 2.4 Simple SELECT Execution

#### 2.4.1 Workflow
1. User selects query from dashboard
2. System displays parameter input form
3. User enters parameter values
4. User clicks "Execute"
5. System validates parameters
6. System executes query with timeout
7. System logs execution
8. System displays results in paginated table
9. User can export to CSV

#### 2.4.2 Results Display
- Server-side pagination (configurable page size: 25, 50, 100)
- Sortable columns
- Column headers from SQL result metadata
- Null values displayed as empty cells
- Timestamps formatted in ISO 8601

#### 2.4.3 CSV Export
- Streaming response (handles large result sets)
- UTF-8 with BOM (Excel compatibility)
- Configurable delimiter (default: comma)
- Headers included (column names)
- Filename: `{query_name}_{timestamp}.csv`

#### 2.4.4 Execution Feedback
- Spinner with elapsed time counter (seconds)
- Cancel button (attempts query cancellation)
- On completion: row count, execution time

### 2.5 UPDATE Workflow

#### 2.5.1 Query Structure
UPDATE workflow queries contain three SQL statements:

```yaml
type: UPDATE_WORKFLOW
selectSql: |
  SELECT id, name, status, modified_date
  FROM customers
  WHERE region = :region AND status = :oldStatus
updateSql: |
  UPDATE customers
  SET status = :newStatus, modified_date = GETDATE()
  WHERE id IN (:ids)
primaryKeyColumn: "id"
backupColumns: ["id", "name", "status", "modified_date"]
rollbackColumns: ["status"]
```

- **selectSql**: Identifies records to be updated (user verification)
- **updateSql**: Performs the update (`:ids` placeholder for PK values)
- **primaryKeyColumn**: Column name for primary key
- **backupColumns**: Columns to backup for audit/reference (all columns shown in preview)
- **rollbackColumns**: Columns to actually restore during rollback (subset of backupColumns, only business data columns that should be reverted)

#### 2.5.2 Workflow Steps (Wizard UI)

**Step 1: Parameters**
- Display parameter input form
- User enters values and clicks "Preview"

**Step 2: Preview**
- Execute SELECT query
- Display affected records in table
- Show record count
- User must click "Approve" to proceed
- Maximum 100,000 rows enforced

**Step 3: Confirm**
- Display summary: record count, update description
- User confirms with "Execute Update" button

**Step 4: Execute**
- Create backup record (JSON blob)
- Execute UPDATE in single transaction
- Log execution
- Display success/failure

**Step 5: Result**
- Show update summary (rows affected)
- Display "Rollback" button (if successful)
- Show backup reference ID

#### 2.5.3 Backup Storage
- Stored in `backup_records` table
- JSON format in BLOB/TEXT column
- Structure:
```json
{
  "columns": ["id", "name", "status", "modified_date"],
  "rows": [
    [1001, "Acme Corp", "A", "2024-01-15T10:30:00"],
    [1002, "Beta Inc", "A", "2024-01-15T11:45:00"]
  ]
}
```
- Metadata stored in separate columns (execution_id, created_by, created_at)

#### 2.5.4 Rollback
- **Trigger**: Manual only, no time limit
- **Scope**: Entire operation (all rows)
- **Usage**: One-time only (marked as "used" after rollback)
- **Flow**:
  1. User clicks "Rollback" on execution detail
  2. System generates rollback SQL preview
  3. User reviews and confirms
  4. System executes rollback in single transaction
  5. System logs rollback execution

**Generated Rollback SQL Pattern** (only `rollbackColumns` are restored):
```sql
UPDATE customers SET status = 'A' WHERE id = 1001;
UPDATE customers SET status = 'A' WHERE id = 1002;
```

Note: The backup contains all `backupColumns` for audit purposes, but only `rollbackColumns` are restored. System-managed columns like `modified_date` are intentionally excluded from rollback.

### 2.6 Execution Logging

#### 2.6.1 Log Entry Fields
| Field | Description |
|-------|-------------|
| id | Unique log entry ID |
| queryId | Reference to query template |
| queryVersion | Version number at execution time |
| connectionName | Database connection used |
| executedBy | Username from AD |
| executedAt | Timestamp (UTC) |
| parameters | JSON object of parameter values |
| rowCount | Number of rows returned/affected |
| executionTimeMs | Duration in milliseconds |
| status | SUCCESS, FAILED, CANCELLED, TIMEOUT |
| errorMessage | Error details (if failed) |
| executionType | SELECT, UPDATE, ROLLBACK |
| backupRecordId | Reference to backup (UPDATE only) |

#### 2.6.2 Log Retention
- Manual retention (no automatic cleanup)
- Admin can export logs to CSV
- Logs are immutable

### 2.7 Configuration Export/Import

#### 2.7.1 Export Format (YAML)

Export contains query metadata plus the embedded config_yaml for each version:

```yaml
version: "1.0"
exportedAt: "2024-01-15T14:30:00Z"
exportedBy: "admin.user"
queries:
  # Simple SELECT query example
  - id: "qry-001"
    name: "Customer Search"
    description: "Search customers by region"
    category: "Customer Queries"           # Simple tag string
    connectionName: "PROD_SQLSERVER"
    version: 3
    config: |                               # Self-contained YAML config
      type: SELECT
      sql: |
        SELECT * FROM customers WHERE region = :region
      config:
        timeoutSeconds: 60
        maxRows: 10000
      parameters:
        - name: region
          label: Region Code
          dataType: STRING
          required: true

  # SELECT query with list parameter example
  - id: "qry-003"
    name: "Customer Lookup by IDs"
    description: "Find customers by list of customer IDs"
    category: "Customer Queries"
    connectionName: "PROD_SQLSERVER"
    version: 1
    config: |
      type: SELECT
      sql: |
        SELECT customer_id, name, email, status
        FROM customers
        WHERE customer_id IN (:customerIds)
      config:
        timeoutSeconds: 60
        maxRows: 10000
      parameters:
        - name: customerIds
          label: Customer IDs (one per line or comma-separated)
          dataType: LIST_STRING
          required: true
          listSeparator: BOTH

  # UPDATE_WORKFLOW query example
  - id: "qry-002"
    name: "Update Customer Status"
    description: "Change customer status by region"
    category: "Customer Queries"
    connectionName: "PROD_SQLSERVER"
    version: 1
    config: |
      type: UPDATE_WORKFLOW
      selectSql: |
        SELECT id, name, status, modified_date
        FROM customers
        WHERE region = :region AND status = :oldStatus
      updateSql: |
        UPDATE customers
        SET status = :newStatus, modified_date = GETDATE()
        WHERE id IN (:ids)
      primaryKeyColumn: id
      backupColumns: [id, name, status, modified_date]
      rollbackColumns: [status]
      config:
        timeoutSeconds: 120
        maxRows: 1000
      parameters:
        - name: region
          label: Region Code
          dataType: STRING
          required: true
        - name: oldStatus
          label: Current Status
          dataType: ENUM
          required: true
          enumValues:
            - value: "A"
              description: "Active"
            - value: "I"
              description: "Inactive"
        - name: newStatus
          label: New Status
          dataType: ENUM
          required: true
          enumValues:
            - value: "A"
              description: "Active"
            - value: "I"
              description: "Inactive"
```

#### 2.7.2 Import Behavior
- **Mode**: Strict merge
- **Conflict Detection**: Fails if query ID exists with different content and same version
- **New Items**: Added automatically
- **Updated Items**: Version must be higher than existing
- **Deleted Items**: Not removed (manual cleanup required)

#### 2.7.3 Environment Modes
- **Test Environment**: Full editing capabilities
- **Prod Environment**: Read-only (import only, no direct edits)
- Controlled by `SQLRUNNER_PROD_MODE=true` environment variable

---

## 3. Technical Architecture

### 3.1 Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Template Engine | Thymeleaf |
| CSS Framework | Tailwind CSS |
| Code Editor | CodeMirror |
| Database Migration | Flyway |
| Build Tool | Maven |
| Container | Docker |

### 3.2 Database Drivers (Bundled)
- Microsoft SQL Server: `mssql-jdbc`
- IBM DB2: `jcc`
- PostgreSQL: `postgresql`

### 3.3 Application Database
- **Platform**: SQL Server (dedicated instance, separate from target databases)
- **Schema**: `sqlrunner`
- **Migration**: Flyway with versioned scripts
- **Purpose**: Stores query templates, categories, execution logs, backups (NOT user data)

Note: The application database is completely separate from the target databases that users query or update. Target databases (SQL Server, DB2, PostgreSQL) are configured as connections and are never modified by the application itself.

### 3.4 Session Management
- Default: HTTP session (in-memory)
- Production-ready: Spring Session with Redis (configuration switch)

### 3.5 Security
- Spring Security with LDAP authentication provider
- CSRF protection enabled
- SQL injection prevention via parameterized queries (NamedParameterJdbcTemplate)
- No direct SQL input from users (templates only)

---

## 4. Database Schema

### 4.1 Entity Relationship

```
queries (1) ----< (N) query_versions
queries (1) ----< (N) execution_logs
execution_logs (1) ----< (0..1) backup_records
```

Note: Categories are stored as simple string tags on queries (no separate table).

### 4.2 Table Definitions

#### queries
```sql
CREATE TABLE sqlrunner.queries (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100) NOT NULL,              -- Simple tag, no FK
    connection_name VARCHAR(100) NOT NULL,
    query_type VARCHAR(20) NOT NULL,             -- 'SELECT' or 'UPDATE_WORKFLOW'
    current_version INT NOT NULL DEFAULT 1,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME2,
    updated_by VARCHAR(100)
);

-- Index for category filtering/grouping
CREATE INDEX idx_queries_category ON sqlrunner.queries(category);
```

#### query_versions
Each version stores the complete query configuration as a self-contained YAML string.

```sql
CREATE TABLE sqlrunner.query_versions (
    id VARCHAR(36) PRIMARY KEY,
    query_id VARCHAR(36) NOT NULL REFERENCES sqlrunner.queries(id),
    version INT NOT NULL,
    config_yaml NVARCHAR(MAX) NOT NULL,          -- Self-contained YAML configuration
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_by VARCHAR(100) NOT NULL,
    UNIQUE(query_id, version)
);
```

**config_yaml** contains the complete query definition:

```yaml
# SELECT query example
type: SELECT
sql: |
  SELECT customer_id, name, email, status
  FROM customers
  WHERE region = :region AND status = :status
config:
  timeoutSeconds: 60
  maxRows: 10000
parameters:
  - name: region
    label: Region Code
    dataType: STRING
    required: true
  - name: status
    label: Customer Status
    dataType: ENUM
    required: true
    enumValues:
      - value: "A"
        description: "Active"
      - value: "I"
        description: "Inactive"
```

```yaml
# UPDATE_WORKFLOW query example
type: UPDATE_WORKFLOW
selectSql: |
  SELECT id, name, status, modified_date
  FROM customers
  WHERE region = :region AND status = :oldStatus
updateSql: |
  UPDATE customers
  SET status = :newStatus, modified_date = GETDATE()
  WHERE id IN (:ids)
primaryKeyColumn: id
backupColumns: [id, name, status, modified_date]
rollbackColumns: [status]
config:
  timeoutSeconds: 120
  maxRows: 1000
parameters:
  - name: region
    label: Region Code
    dataType: STRING
    required: true
  - name: oldStatus
    label: Current Status
    dataType: ENUM
    required: true
    enumValues:
      - value: "A"
        description: "Active"
      - value: "I"
        description: "Inactive"
  - name: newStatus
    label: New Status
    dataType: ENUM
    required: true
    enumValues:
      - value: "A"
        description: "Active"
      - value: "I"
        description: "Inactive"
```

```yaml
# LIST parameter example
type: SELECT
sql: |
  SELECT customer_id, name, email
  FROM customers
  WHERE customer_id IN (:customerIds)
config:
  timeoutSeconds: 60
  maxRows: 10000
parameters:
  - name: customerIds
    label: Customer IDs (one per line or comma-separated)
    dataType: LIST_STRING
    required: true
    listSeparator: BOTH
```

#### execution_logs
```sql
CREATE TABLE sqlrunner.execution_logs (
    id VARCHAR(36) PRIMARY KEY,
    query_id VARCHAR(36) NOT NULL REFERENCES sqlrunner.queries(id),
    query_version INT NOT NULL,
    connection_name VARCHAR(100) NOT NULL,
    executed_by VARCHAR(100) NOT NULL,
    executed_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    parameters NVARCHAR(MAX), -- JSON
    row_count INT,
    execution_time_ms BIGINT,
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, CANCELLED, TIMEOUT
    error_message NVARCHAR(MAX),
    execution_type VARCHAR(20) NOT NULL, -- SELECT, UPDATE, ROLLBACK
    backup_record_id VARCHAR(36)
);
```

#### backup_records
```sql
CREATE TABLE sqlrunner.backup_records (
    id VARCHAR(36) PRIMARY KEY,
    execution_log_id VARCHAR(36) NOT NULL REFERENCES sqlrunner.execution_logs(id),
    backup_data NVARCHAR(MAX) NOT NULL, -- JSON blob
    row_count INT NOT NULL,
    is_rolled_back BIT NOT NULL DEFAULT 0,
    rolled_back_at DATETIME2,
    rolled_back_by VARCHAR(100),
    rollback_execution_log_id VARCHAR(36),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE()
);
```

---

## 5. User Interface

### 5.1 Color Scheme (RBC-inspired)
| Element | Color |
|---------|-------|
| Primary (headers, buttons) | #003168 (Royal Blue) |
| Accent (highlights, CTAs) | #FFD200 (Yellow) |
| Background | #FFFFFF (White) |
| Secondary Background | #F5F5F5 (Light Gray) |
| Text Primary | #333333 (Dark Gray) |
| Text Secondary | #666666 (Medium Gray) |
| Success | #28A745 (Green) |
| Error | #DC3545 (Red) |
| Warning | #FFC107 (Amber) |

### 5.2 Page Structure

#### 5.2.1 Layout
- Fixed header with logo, navigation, user info
- Sidebar navigation (collapsible)
- Main content area
- Footer with version info

#### 5.2.2 Navigation
- **Dashboard** (home icon)
- **Queries** (by category, expandable)
- **History** (execution logs)
- **Admin** (visible to ADMIN role only)
  - Query Templates
  - Export/Import

### 5.3 Key Screens

#### Login Page
- Clean form: username, password, login button
- Error message display
- Company branding

#### Dashboard
- **Recent Executions** (top section): Last 10 executions with status
- **Queries by Category** (main section): Accordion/card layout
  - Category name and description
  - List of queries with name, type badge (SELECT/UPDATE), description

#### Query Execution (SELECT)
- Query name and description header
- Parameter form (dynamic based on parameter definitions)
- Execute button
- Results table with pagination controls
- Export CSV button
- Execution stats (time, row count)

#### Update Wizard
- Step indicator (1-5)
- Step 1: Parameter form
- Step 2: Preview table with row count, Approve/Cancel buttons
- Step 3: Confirmation summary, Execute/Back buttons
- Step 4: Progress spinner with elapsed time, Cancel button
- Step 5: Result summary, Rollback button (if applicable)

#### Execution History
- Filterable table:
  - Date range picker
  - User dropdown
  - Query dropdown
  - Status dropdown
- Columns: Timestamp, Query, User, Type, Status, Duration, Row Count, Actions
- Click row for detail view

#### Admin: Query Editor
- Left panel: Query list (grouped by category)
- Right panel: Editor
  - Metadata form:
    - Name, Description
    - Category (autocomplete from existing, or type new)
    - Connection (dropdown from configured connections)
  - CodeMirror YAML editor for config (syntax highlighting)
    - Contains: type, SQL, parameters, config options
    - Self-contained query definition
  - Version history collapsible section
  - Save (creates new version) button

---

## 6. Configuration

### 6.1 Application Configuration (application.yml)

```yaml
server:
  port: 8080
  servlet:
    session:
      timeout: 20m

spring:
  datasource:
    url: jdbc:sqlserver://${SQLRUNNER_DB_HOST}:1433;databaseName=${SQLRUNNER_DB_NAME};schema=sqlrunner
    username: ${SQLRUNNER_DB_USER}
    password: ${SQLRUNNER_DB_PASSWORD}
  flyway:
    enabled: true
    schemas: sqlrunner
    baseline-on-migrate: true

sqlrunner:
  prod-mode: ${SQLRUNNER_PROD_MODE:false}
  default-timeout-seconds: 30
  default-max-rows: 100000
  max-update-rows: 100000
  csv:
    default-delimiter: ","
    include-bom: true

  ldap:
    url: ${LDAP_URL}
    base-dn: ${LDAP_BASE_DN}
    user-search-base: "ou=Users"
    user-search-filter: "(sAMAccountName={0})"
    group-search-base: "ou=Groups"
    group-role-mapping:
      "CN=SQL-Admins,OU=Groups,DC=corp,DC=example,DC=com": "ADMIN"
      "CN=SQL-UpdateRunners,OU=Groups,DC=corp,DC=example,DC=com": "UPDATE_RUNNER"
      "CN=SQL-Users,OU=Groups,DC=corp,DC=example,DC=com": "SELECT_RUNNER"

  connections:
    - name: PROD_SQLSERVER
      type: SQLSERVER
      display-name: "Production SQL Server"
      jdbc-url: "jdbc:sqlserver://${PROD_SQL_HOST}:1433;databaseName=${PROD_SQL_DB}"
      credentials-env-prefix: PROD_SQL
    - name: PROD_DB2
      type: DB2
      display-name: "Production DB2"
      jdbc-url: "jdbc:db2://${PROD_DB2_HOST}:50000/${PROD_DB2_DB}"
      credentials-env-prefix: PROD_DB2
    - name: PROD_POSTGRES
      type: POSTGRESQL
      display-name: "Production PostgreSQL"
      jdbc-url: "jdbc:postgresql://${PROD_PG_HOST}:5432/${PROD_PG_DB}"
      credentials-env-prefix: PROD_PG

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: when-authorized
```

### 6.2 Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| SQLRUNNER_DB_HOST | App database host | Yes |
| SQLRUNNER_DB_NAME | App database name | Yes |
| SQLRUNNER_DB_USER | App database user | Yes |
| SQLRUNNER_DB_PASSWORD | App database password | Yes |
| SQLRUNNER_PROD_MODE | Enable prod mode (disables editing) | No |
| LDAP_URL | LDAP server URL | Yes |
| LDAP_BASE_DN | LDAP base DN | Yes |
| PROD_SQL_HOST | SQL Server host | Per connection |
| PROD_SQL_DB | SQL Server database | Per connection |
| PROD_SQL_USER | SQL Server username | Per connection |
| PROD_SQL_PASSWORD | SQL Server password | Per connection |
| (similar for DB2 and PostgreSQL) | | |

---

## 7. Deployment

### 7.1 Docker

#### Dockerfile
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/sql-runner-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### docker-compose.yml (Development)
```yaml
version: '3.8'
services:
  sql-runner:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SQLRUNNER_DB_HOST=host.docker.internal
      - SQLRUNNER_DB_NAME=sqlrunner
      - SQLRUNNER_DB_USER=sa
      - SQLRUNNER_DB_PASSWORD=YourPassword
      - LDAP_URL=ldap://ldap.example.com:389
      - LDAP_BASE_DN=DC=corp,DC=example,DC=com
      # ... other env vars
    volumes:
      - ./logs:/app/logs
```

### 7.2 OpenShift Deployment
- Deploy as single container
- Use OpenShift Secrets for database credentials
- Secrets mounted as environment variables
- Liveness/Readiness probes pointing to `/actuator/health`

---

## 8. API Endpoints (Internal)

### 8.1 User-Facing (Thymeleaf)
| Method | Path | Description |
|--------|------|-------------|
| GET | / | Dashboard |
| GET | /login | Login page |
| POST | /login | Process login |
| GET | /logout | Logout |
| GET | /query/{id} | Query execution page |
| POST | /query/{id}/execute | Execute SELECT query |
| GET | /query/{id}/results | Paginated results (AJAX) |
| GET | /query/{id}/export | CSV download |
| GET | /update/{id} | Update wizard |
| POST | /update/{id}/preview | Step 2: Preview |
| POST | /update/{id}/execute | Step 4: Execute update |
| POST | /update/{id}/rollback | Rollback execution |
| GET | /history | Execution history |
| GET | /history/{logId} | Execution detail |

### 8.2 Admin (Thymeleaf)
| Method | Path | Description |
|--------|------|-------------|
| GET | /admin/queries | Query list |
| GET | /admin/queries/categories | Get distinct categories (for autocomplete) |
| GET | /admin/queries/new | New query form |
| POST | /admin/queries | Create query |
| GET | /admin/queries/{id} | Edit query |
| PUT | /admin/queries/{id} | Update query |
| GET | /admin/queries/{id}/versions | Version history |
| GET | /admin/connections/test/{name} | Test connection |
| GET | /admin/export | Export page |
| POST | /admin/export | Download YAML |
| GET | /admin/import | Import page |
| POST | /admin/import | Upload and process YAML |

---

## 9. Testing Strategy

### 9.1 Unit Tests
- Service layer with mocked repositories
- Parameter validation logic
- SQL generation for rollback
- YAML serialization/deserialization

### 9.2 Integration Tests
- Repository tests with TestContainers (SQL Server)
- Controller tests with MockMvc
- LDAP authentication with embedded LDAP server

### 9.3 Manual Testing Checklist
1. Login with valid AD credentials
2. Login failure with invalid credentials
3. Session timeout after 20 minutes
4. Execute SELECT query with all parameter types
5. Verify pagination on large result sets
6. Export results to CSV
7. Complete UPDATE workflow (all 5 steps)
8. Rollback an UPDATE operation
9. Verify rollback is one-time only
10. Create/edit category (as ADMIN)
11. Create/edit query with parameters (as ADMIN)
12. Export configuration to YAML
13. Import configuration from YAML
14. Test connection button
15. Cancel long-running query
16. Access restrictions by role

---

## 10. Future Considerations (Out of Scope)

- Redis session management (architecture prepared)
- Statement-level permissions per AD group
- Scheduled query execution
- Email notifications
- Query result caching
- Bulk operations
- API access (REST/GraphQL)
