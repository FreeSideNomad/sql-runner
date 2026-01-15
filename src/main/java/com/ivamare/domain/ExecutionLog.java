package com.ivamare.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Entity representing an execution log entry. */
@Entity
@Table(name = "execution_logs", schema = "sqlrunner")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLog {

  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "query_id", nullable = false, length = 36)
  private String queryId;

  @Column(name = "query_version", nullable = false)
  private Integer queryVersion;

  @Column(name = "connection_name", nullable = false, length = 100)
  private String connectionName;

  @Column(name = "executed_by", nullable = false, length = 100)
  private String executedBy;

  @Column(name = "executed_at", nullable = false)
  private LocalDateTime executedAt;

  @Column(name = "parameters", columnDefinition = "TEXT")
  private String parameters;

  @Column(name = "row_count")
  private Integer rowCount;

  @Column(name = "execution_time_ms")
  private Long executionTimeMs;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExecutionStatus status;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Enumerated(EnumType.STRING)
  @Column(name = "execution_type", nullable = false, length = 20)
  private ExecutionType executionType;

  @Column(name = "backup_record_id", length = 36)
  private String backupRecordId;

  @PrePersist
  protected void onCreate() {
    if (executedAt == null) {
      executedAt = LocalDateTime.now();
    }
  }
}
