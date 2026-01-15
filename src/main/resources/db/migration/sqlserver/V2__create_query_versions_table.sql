-- V2: Create query_versions table
-- Stores versioned query configurations

CREATE TABLE sqlrunner.query_versions (
    id NVARCHAR(36) PRIMARY KEY,
    query_id NVARCHAR(36) NOT NULL,
    version INT NOT NULL,
    config_yaml NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_by NVARCHAR(100) NOT NULL,
    CONSTRAINT fk_query_versions_query FOREIGN KEY (query_id)
        REFERENCES sqlrunner.queries(id) ON DELETE CASCADE,
    CONSTRAINT uq_query_version UNIQUE (query_id, version)
);
GO

CREATE INDEX idx_query_versions_query ON sqlrunner.query_versions(query_id);
GO
