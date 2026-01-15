-- V2: Create query_versions table (H2)
-- Stores versioned query configurations

CREATE TABLE sqlrunner.query_versions (
    id VARCHAR(36) PRIMARY KEY,
    query_id VARCHAR(36) NOT NULL,
    version INT NOT NULL,
    config_yaml TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_query_versions_query FOREIGN KEY (query_id)
        REFERENCES sqlrunner.queries(id) ON DELETE CASCADE,
    CONSTRAINT uq_query_version UNIQUE (query_id, version)
);

CREATE INDEX idx_query_versions_query ON sqlrunner.query_versions(query_id);
