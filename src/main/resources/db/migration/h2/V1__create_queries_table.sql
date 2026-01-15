-- V1: Create queries table (H2)
-- Stores query template metadata

CREATE SCHEMA IF NOT EXISTS sqlrunner;

CREATE TABLE sqlrunner.queries (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100) NOT NULL,
    connection_name VARCHAR(100) NOT NULL,
    query_type VARCHAR(20) NOT NULL,
    current_version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_queries_category ON sqlrunner.queries(category);
CREATE INDEX idx_queries_active ON sqlrunner.queries(is_active);
CREATE INDEX idx_queries_type ON sqlrunner.queries(query_type);
