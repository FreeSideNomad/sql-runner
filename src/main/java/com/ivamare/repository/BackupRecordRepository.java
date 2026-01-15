package com.ivamare.repository;

import com.ivamare.domain.BackupRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for BackupRecord entities. */
@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, String> {

  Optional<BackupRecord> findByExecutionLogId(String executionLogId);

  List<BackupRecord> findByIsRolledBackFalse();

  List<BackupRecord> findByIsRolledBackTrue();

  Optional<BackupRecord> findByRollbackExecutionLogId(String rollbackExecutionLogId);
}
