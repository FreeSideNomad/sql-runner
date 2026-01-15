-- V3: Create execution_logs table
-- Stores audit trail for all query executions

CREATE TABLE sqlrunner.execution_logs (
    id NVARCHAR(36) PRIMARY KEY,
    query_id NVARCHAR(36) NOT NULL,
    query_version INT NOT NULL,
    connection_name NVARCHAR(100) NOT NULL,
    executed_by NVARCHAR(100) NOT NULL,
    executed_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    parameters NVARCHAR(MAX),
    row_count INT,
    execution_time_ms BIGINT,
    status NVARCHAR(20) NOT NULL,
    error_message NVARCHAR(MAX),
    execution_type NVARCHAR(20) NOT NULL,
    backup_record_id NVARCHAR(36),
    CONSTRAINT fk_execution_logs_query FOREIGN KEY (query_id)
        REFERENCES sqlrunner.queries(id)
);
GO

CREATE INDEX idx_execution_logs_query ON sqlrunner.execution_logs(query_id);
CREATE INDEX idx_execution_logs_user ON sqlrunner.execution_logs(executed_by);
CREATE INDEX idx_execution_logs_date ON sqlrunner.execution_logs(executed_at);
CREATE INDEX idx_execution_logs_status ON sqlrunner.execution_logs(status);
CREATE INDEX idx_execution_logs_type ON sqlrunner.execution_logs(execution_type);
GO
