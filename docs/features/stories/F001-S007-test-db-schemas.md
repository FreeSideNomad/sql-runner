# F001-S007: Create Test Database Schemas

## User Story

**As a** developer
**I want** identical test schemas (customers, accounts, transactions) across all three databases
**So that** I can write cross-database E2E tests

## Acceptance Criteria

- [ ] Given SQL Server init script, then customers/accounts/transactions tables are created
- [ ] Given PostgreSQL init script, then customers/accounts/transactions tables are created
- [ ] Given DB2 init script, then customers/accounts/transactions tables are created
- [ ] Given all schemas, then table structures are identical (same columns, types, constraints)
- [ ] Given all schemas, then appropriate indexes exist for common queries

## Technical Notes

### Files to Create
- `src/test/resources/test-data/sqlserver-init.sql`
- `src/test/resources/test-data/postgres-init.sql`
- `src/test/resources/test-data/db2-init.sql`

### Schema Definition (adapt syntax per database)
```sql
CREATE TABLE customers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    status VARCHAR(20) NOT NULL,        -- ACTIVE, INACTIVE, PENDING
    region VARCHAR(50),                 -- NA, EU, APAC
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE accounts (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_type VARCHAR(20) NOT NULL,  -- CHECKING, SAVINGS, CREDIT
    balance DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,       -- USD, EUR, GBP
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE transactions (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,  -- DEPOSIT, WITHDRAWAL, TRANSFER
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(500),
    reference_id VARCHAR(100),
    executed_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,        -- COMPLETED, PENDING, FAILED
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);
```

### Database-Specific Syntax Notes
- **SQL Server**: Use `DATETIME2` instead of `TIMESTAMP`, `NVARCHAR` for Unicode
- **PostgreSQL**: Standard SQL syntax works
- **DB2**: May need `TIMESTAMP(6)`, check VARCHAR limits

### Indexes
```sql
CREATE INDEX idx_customers_region ON customers(region);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_executed ON transactions(executed_at);
```

## Test Plan

- [ ] Integration test: Schema scripts execute without errors on each DB
- [ ] Integration test: Foreign key constraints are enforced
- [ ] Integration test: Indexes are created

## Parent Feature

Relates to F001-dev-setup
