-- V1: Create queries table
-- Stores query template metadata

IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = 'sqlrunner')
BEGIN
    EXEC('CREATE SCHEMA sqlrunner');
END
GO

CREATE TABLE sqlrunner.queries (
    id NVARCHAR(36) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(1000),
    category NVARCHAR(100) NOT NULL,
    connection_name NVARCHAR(100) NOT NULL,
    query_type NVARCHAR(20) NOT NULL,
    current_version INT NOT NULL DEFAULT 1,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_by NVARCHAR(100) NOT NULL,
    updated_at DATETIME2,
    updated_by NVARCHAR(100)
);
GO

CREATE INDEX idx_queries_category ON sqlrunner.queries(category);
CREATE INDEX idx_queries_active ON sqlrunner.queries(is_active);
CREATE INDEX idx_queries_type ON sqlrunner.queries(query_type);
GO
