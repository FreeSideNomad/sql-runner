-- V4: Create backup_records table (H2)
-- Stores backup data for UPDATE workflow rollback capability

CREATE TABLE sqlrunner.backup_records (
    id VARCHAR(36) PRIMARY KEY,
    execution_log_id VARCHAR(36) NOT NULL,
    backup_data TEXT NOT NULL,
    row_count INT NOT NULL,
    is_rolled_back BOOLEAN NOT NULL DEFAULT FALSE,
    rolled_back_at TIMESTAMP,
    rolled_back_by VARCHAR(100),
    rollback_execution_log_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_backup_records_execution FOREIGN KEY (execution_log_id)
        REFERENCES sqlrunner.execution_logs(id)
);

CREATE INDEX idx_backup_records_execution ON sqlrunner.backup_records(execution_log_id);
CREATE INDEX idx_backup_records_rolled_back ON sqlrunner.backup_records(is_rolled_back);
