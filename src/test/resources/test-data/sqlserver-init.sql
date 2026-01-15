-- SQL Server schema for TestContainers

CREATE TABLE customers (
    id NVARCHAR(36) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    email NVARCHAR(200),
    status NVARCHAR(20) NOT NULL,
    region NVARCHAR(50),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2
);

CREATE TABLE accounts (
    id NVARCHAR(36) PRIMARY KEY,
    customer_id NVARCHAR(36) NOT NULL,
    account_number NVARCHAR(50) NOT NULL,
    account_type NVARCHAR(20) NOT NULL,
    balance DECIMAL(15,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL,
    opened_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    closed_at DATETIME2,
    CONSTRAINT FK_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE transactions (
    id NVARCHAR(36) PRIMARY KEY,
    account_id NVARCHAR(36) NOT NULL,
    transaction_type NVARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description NVARCHAR(500),
    reference_id NVARCHAR(100),
    executed_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    status NVARCHAR(20) NOT NULL,
    CONSTRAINT FK_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_customers_region ON customers(region);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_executed ON transactions(executed_at);
