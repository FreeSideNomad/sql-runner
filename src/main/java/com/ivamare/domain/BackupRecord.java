package com.ivamare.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Entity representing a backup record for UPDATE workflow rollback. */
@Entity
@Table(name = "backup_records", schema = "sqlrunner")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupRecord {

  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "execution_log_id", nullable = false, length = 36)
  private String executionLogId;

  @Column(name = "backup_data", nullable = false, columnDefinition = "TEXT")
  private String backupData;

  @Column(name = "row_count", nullable = false)
  private Integer rowCount;

  @Column(name = "is_rolled_back", nullable = false)
  @Builder.Default
  private Boolean isRolledBack = false;

  @Column(name = "rolled_back_at")
  private LocalDateTime rolledBackAt;

  @Column(name = "rolled_back_by", length = 100)
  private String rolledBackBy;

  @Column(name = "rollback_execution_log_id", length = 36)
  private String rollbackExecutionLogId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
