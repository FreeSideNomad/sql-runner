-- V3: Create execution_logs table (H2)
-- Stores audit trail for all query executions

CREATE TABLE sqlrunner.execution_logs (
    id VARCHAR(36) PRIMARY KEY,
    query_id VARCHAR(36) NOT NULL,
    query_version INT NOT NULL,
    connection_name VARCHAR(100) NOT NULL,
    executed_by VARCHAR(100) NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parameters TEXT,
    row_count INT,
    execution_time_ms BIGINT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    execution_type VARCHAR(20) NOT NULL,
    backup_record_id VARCHAR(36),
    CONSTRAINT fk_execution_logs_query FOREIGN KEY (query_id)
        REFERENCES sqlrunner.queries(id)
);

CREATE INDEX idx_execution_logs_query ON sqlrunner.execution_logs(query_id);
CREATE INDEX idx_execution_logs_user ON sqlrunner.execution_logs(executed_by);
CREATE INDEX idx_execution_logs_date ON sqlrunner.execution_logs(executed_at);
CREATE INDEX idx_execution_logs_status ON sqlrunner.execution_logs(status);
CREATE INDEX idx_execution_logs_type ON sqlrunner.execution_logs(execution_type);
