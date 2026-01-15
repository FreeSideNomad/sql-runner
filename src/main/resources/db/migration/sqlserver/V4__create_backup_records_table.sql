-- V4: Create backup_records table
-- Stores backup data for UPDATE workflow rollback capability

CREATE TABLE sqlrunner.backup_records (
    id NVARCHAR(36) PRIMARY KEY,
    execution_log_id NVARCHAR(36) NOT NULL,
    backup_data NVARCHAR(MAX) NOT NULL,
    row_count INT NOT NULL,
    is_rolled_back BIT NOT NULL DEFAULT 0,
    rolled_back_at DATETIME2,
    rolled_back_by NVARCHAR(100),
    rollback_execution_log_id NVARCHAR(36),
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_backup_records_execution FOREIGN KEY (execution_log_id)
        REFERENCES sqlrunner.execution_logs(id)
);
GO

CREATE INDEX idx_backup_records_execution ON sqlrunner.backup_records(execution_log_id);
CREATE INDEX idx_backup_records_rolled_back ON sqlrunner.backup_records(is_rolled_back);
GO
