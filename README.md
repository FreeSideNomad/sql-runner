# SQL Runner

A Spring Boot 3.5.9 web application for managing and executing SQL queries with support for multiple database connections, parameterized queries, UPDATE workflows with backup/rollback capabilities, and execution history tracking.

## Table of Contents

- [Quick Start](#quick-start)
- [Database Connection Configuration](#database-connection-configuration)
- [User Guide](#user-guide)
- [Security & Roles](#security--roles)
- [Configuration Reference](#configuration-reference)
- [Development](#development)

---

## Quick Start

### Prerequisites

- Java 21
- Maven 3.8+
- Docker (for local database instances)

### Running Locally

1. **Start local databases** (optional - for testing against real databases):
   ```bash
   docker-compose up -d
   ```

2. **Set database credentials** as environment variables:
   ```bash
   export LOCAL_SQLSERVER_USER=sa
   export LOCAL_SQLSERVER_PASSWORD='SqlRunner123!'
   export LOCAL_POSTGRES_USER=sqlrunner
   export LOCAL_POSTGRES_PASSWORD='SqlRunner123!'
   export LOCAL_DB2_USER=db2inst1
   export LOCAL_DB2_PASSWORD='SqlRunner123!'
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application** at http://localhost:9090

### Default Users (Development)

| Username | Password | Role | Capabilities |
|----------|----------|------|--------------|
| admin | admin | ADMIN | Full access, import/export queries |
| updater | updater | UPDATE_RUNNER | Execute UPDATE workflows |
| reader | reader | SELECT_RUNNER | Execute SELECT queries only |

---

## Database Connection Configuration

SQL Runner connects to multiple databases for query execution. Connections are configured in `application.yml` with credentials resolved from environment variables.

### Configuration Structure

```yaml
sqlrunner:
  connections:
    databases:
      <connection-id>:          # Unique identifier (e.g., "prod-orders-db")
        name: Display Name       # Human-readable name shown in UI
        type: SQLSERVER          # Database type: SQLSERVER, POSTGRES, DB2, H2
        host: db.example.com     # Database host
        port: 1433               # Port (optional, uses default for type)
        database: orders         # Database name
        schema: dbo              # Schema (optional)
        credentialPrefix: PROD_ORDERS  # Environment variable prefix
        properties:              # Additional JDBC properties (optional)
          applicationName: sql-runner
        pool:                    # Connection pool settings (optional)
          maximumPoolSize: 10
          minimumIdle: 2
          connectionTimeout: 30000
          idleTimeout: 600000
          maxLifetime: 1800000
```

### Credential Resolution

Credentials are resolved from environment variables using the `credentialPrefix`:
- `{credentialPrefix}_USER` - Database username
- `{credentialPrefix}_PASSWORD` - Database password

**Example:**
```yaml
credentialPrefix: PROD_ORDERS
```
Resolves from:
- `PROD_ORDERS_USER`
- `PROD_ORDERS_PASSWORD`

### Supported Database Types

| Type | Driver | Default Port | Validation Query |
|------|--------|--------------|------------------|
| SQLSERVER | com.microsoft.sqlserver.jdbc.SQLServerDriver | 1433 | SELECT 1 |
| POSTGRES | org.postgresql.Driver | 5432 | SELECT 1 |
| DB2 | com.ibm.db2.jcc.DB2Driver | 50000 | SELECT 1 FROM SYSIBM.SYSDUMMY1 |
| H2 | org.h2.Driver | - | SELECT 1 |

### Complete Example

```yaml
sqlrunner:
  connections:
    databases:
      # Production SQL Server
      prod-orders:
        name: Production Orders DB
        type: SQLSERVER
        host: orders-db.prod.company.com
        port: 1433
        database: OrdersDB
        schema: dbo
        credentialPrefix: PROD_ORDERS
        pool:
          maximumPoolSize: 5
          minimumIdle: 1

      # Production PostgreSQL
      prod-analytics:
        name: Analytics DB
        type: POSTGRES
        host: analytics.prod.company.com
        database: analytics
        schema: public
        credentialPrefix: PROD_ANALYTICS

      # Production DB2
      prod-mainframe:
        name: Mainframe DB2
        type: DB2
        host: mainframe.company.com
        port: 50000
        database: PRODDB
        credentialPrefix: PROD_MAINFRAME
```

### Setting Credentials

**Linux/macOS:**
```bash
export PROD_ORDERS_USER=app_user
export PROD_ORDERS_PASSWORD='SecurePass123!'
export PROD_ANALYTICS_USER=analytics_reader
export PROD_ANALYTICS_PASSWORD='AnalyticsPass!'
```

**Windows (PowerShell):**
```powershell
$env:PROD_ORDERS_USER = "app_user"
$env:PROD_ORDERS_PASSWORD = "SecurePass123!"
```

**Docker/Kubernetes:**
```yaml
env:
  - name: PROD_ORDERS_USER
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: orders-user
  - name: PROD_ORDERS_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: orders-password
```

---

## User Guide

### Query Types

SQL Runner supports two query types:

#### 1. SELECT Queries

Simple read-only queries that display results in a table with CSV export.

**Features:**
- Parameterized queries with type validation
- CSV export of results
- Configurable timeout and max rows

#### 2. UPDATE_WORKFLOW Queries

Multi-step workflow for safe data updates with backup and rollback.

**Workflow Steps:**
1. **Preview** - Execute SELECT to see affected rows
2. **Confirm** - Review preview data before proceeding
3. **Backup** - System backs up original values
4. **Execute** - Run UPDATE statement
5. **Rollback** - Restore original values if needed

### Creating a Query

1. Navigate to **Queries** > **New Query**
2. Fill in:
   - **Name**: Descriptive query name
   - **Description**: What the query does
   - **Connection**: Target database
   - **Query Type**: SELECT or UPDATE_WORKFLOW
3. Configure the query using the YAML editor or form fields

### Query Configuration

#### SELECT Query Example

```yaml
sql: |
  SELECT id, name, email, status, created_at
  FROM customers
  WHERE region = :region AND status = :status
timeoutSeconds: 30
maxRows: 1000
parameters:
  - name: region
    label: Region
    dataType: ENUM
    required: true
    enumValues:
      - value: EAST
        description: Eastern Region
      - value: WEST
        description: Western Region
  - name: status
    label: Customer Status
    dataType: STRING
    required: true
    defaultValue: ACTIVE
```

#### UPDATE_WORKFLOW Query Example

```yaml
selectSql: |
  SELECT id, name, status, email
  FROM customers
  WHERE region = :region AND status = :oldStatus
updateSql: |
  UPDATE customers
  SET status = :newStatus, modified_at = GETDATE()
  WHERE id IN (:id_list)
updateBindingMode: BATCH
primaryKeyColumn: id
backupColumns:
  - id
  - name
  - status
  - modified_at
rollbackColumns:
  - status
parameters:
  - name: region
    label: Region
    dataType: STRING
    required: true
  - name: oldStatus
    label: Current Status
    dataType: STRING
    required: true
  - name: newStatus
    label: New Status
    dataType: STRING
    required: true
```

### Parameter Data Types

| Type | Description | Validation |
|------|-------------|------------|
| STRING | Text input | Optional regex |
| INTEGER | Whole numbers | Numeric only |
| DECIMAL | Decimal numbers | Numeric with decimals |
| DATE | Date picker | YYYY-MM-DD format |
| DATETIME | Date/time picker | ISO 8601 format |
| BOOLEAN | True/false | Checkbox |
| ENUM | Dropdown selection | Predefined values |
| LIST | Comma-separated values | Split by separator |

### UPDATE Binding Modes

When creating UPDATE_WORKFLOW queries, select a binding mode:

| Mode | Description | Use Case |
|------|-------------|----------|
| **STANDARD** | Uses only user-input parameters | Simple updates where SELECT and UPDATE use same conditions |
| **BATCH** | Collects preview IDs into `:id_list` | Update only previewed rows with single UPDATE |
| **ROW_BY_ROW** | Executes UPDATE per row using column values | Per-row transformations (e.g., UPPER(:name)) |

**BATCH Mode Example:**
```yaml
updateSql: UPDATE customers SET status = :newStatus WHERE id IN (:id_list)
updateBindingMode: BATCH
primaryKeyColumn: id
```

**ROW_BY_ROW Mode Example:**
```yaml
updateSql: UPDATE customers SET name = UPPER(:name) WHERE id = :id
updateBindingMode: ROW_BY_ROW
primaryKeyColumn: id
```

### Viewing Execution History

Navigate to **History** to see all query executions including:
- Execution status (SUCCESS, FAILED, TIMEOUT)
- Duration and row count
- Parameters used
- For UPDATE workflows: backup data and rollback status

From the history detail page you can:
- Download backup data as CSV
- Download rollback script
- Execute rollback (if not already rolled back)

### Import/Export Queries

Administrators can import/export query definitions:
- **Export**: Admin > Export Queries (JSON format)
- **Import**: Admin > Import Queries (upload JSON)

---

## Security & Roles

### Role Hierarchy

```
ADMIN
  └── UPDATE_RUNNER
        └── SELECT_RUNNER
```

Higher roles inherit all permissions from lower roles.

### Permissions

| Action | SELECT_RUNNER | UPDATE_RUNNER | ADMIN |
|--------|---------------|---------------|-------|
| View queries | Yes | Yes | Yes |
| Execute SELECT | Yes | Yes | Yes |
| Create/edit queries | Yes | Yes | Yes |
| Execute UPDATE | No | Yes | Yes |
| Execute rollback | No | Yes | Yes |
| Import/export queries | No | No | Yes |
| Admin functions | No | No | Yes |

### Production Security

For production, configure LDAP/AD authentication by mapping AD groups to roles:

```yaml
sqlrunner:
  security:
    role-mapping:
      ADMIN: CN=SQLRunner-Admins,OU=Groups,DC=company,DC=com
      UPDATE_RUNNER: CN=SQLRunner-UpdateRunners,OU=Groups,DC=company,DC=com
      SELECT_RUNNER: CN=SQLRunner-SelectRunners,OU=Groups,DC=company,DC=com
```

---

## Configuration Reference

### Application Settings

```yaml
sqlrunner:
  read-only-mode: false           # Disable all UPDATE operations
  execution:
    default-timeout-seconds: 60   # Default query timeout
  update:
    max-affected-rows: 100000     # Maximum rows for UPDATE operations
```

### Server Settings

```yaml
server:
  port: 9090                      # Application port
  servlet:
    session:
      timeout: 20m                # Session timeout
```

### Spring Profiles

| Profile | Database | Authentication | Use Case |
|---------|----------|----------------|----------|
| default | H2 (in-memory) | In-memory users | Local development |
| dev | SQL Server | In-memory users | Development with Docker |
| test | H2 (in-memory) | In-memory users | Automated tests |
| prod | Configured DBs | LDAP/AD | Production |

### Environment Variables Summary

| Variable | Description |
|----------|-------------|
| `{PREFIX}_USER` | Database username for connection |
| `{PREFIX}_PASSWORD` | Database password for connection |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile |
| `SERVER_PORT` | Override application port |

---

## Development

### Building

```bash
mvn clean package
```

### Running Tests

```bash
mvn test                    # Unit tests
mvn verify                  # All tests
mvn verify -Pcoverage       # Tests with 80% coverage check
```

### Code Formatting

```bash
mvn spotless:apply          # Format code
mvn spotless:check          # Check formatting
```

### Local Development with Docker

Start all local databases:
```bash
docker-compose up -d
```

Stop and remove data:
```bash
docker-compose down -v
```

**Apple Silicon Note:** Enable "Use Rosetta for x86_64/amd64 emulation" in Docker Desktop for SQL Server and DB2.

### Project Structure

```
src/main/java/com/ivamare/
├── config/           # Configuration classes
├── controller/       # Web controllers
├── domain/           # Entity classes
├── dto/              # Data transfer objects
├── repository/       # Data access layer
├── service/          # Business logic
└── util/             # Utility classes

src/main/resources/
├── application.yml   # Main configuration
├── db/migration/     # Flyway migrations
└── templates/        # Thymeleaf templates
```

---

## License

Proprietary - All rights reserved.
