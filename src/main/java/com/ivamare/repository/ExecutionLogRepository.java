package com.ivamare.repository;

import com.ivamare.domain.ExecutionLog;
import com.ivamare.domain.ExecutionStatus;
import com.ivamare.domain.ExecutionType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for ExecutionLog entities. */
@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, String> {

  Page<ExecutionLog> findByExecutedByOrderByExecutedAtDesc(String executedBy, Pageable pageable);

  Page<ExecutionLog> findByQueryIdOrderByExecutedAtDesc(String queryId, Pageable pageable);

  List<ExecutionLog> findTop10ByExecutedByOrderByExecutedAtDesc(String executedBy);

  List<ExecutionLog> findByQueryIdAndExecutionType(String queryId, ExecutionType executionType);

  @Query(
      "SELECT e FROM ExecutionLog e WHERE "
          + "(:user IS NULL OR e.executedBy = :user) AND "
          + "(:queryId IS NULL OR e.queryId = :queryId) AND "
          + "(:status IS NULL OR e.status = :status) AND "
          + "(:executionType IS NULL OR e.executionType = :executionType) AND "
          + "(:startDate IS NULL OR e.executedAt >= :startDate) AND "
          + "(:endDate IS NULL OR e.executedAt <= :endDate) "
          + "ORDER BY e.executedAt DESC")
  Page<ExecutionLog> findWithFilters(
      @Param("user") String user,
      @Param("queryId") String queryId,
      @Param("status") ExecutionStatus status,
      @Param("executionType") ExecutionType executionType,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  long countByStatus(ExecutionStatus status);

  long countByExecutionType(ExecutionType executionType);
}
