package com.ivamare.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ExecutionLogRepositoryTest {

  @Autowired private ExecutionLogRepository executionLogRepository;

  @Autowired private QueryRepository queryRepository;

  private Query testQuery;

  @BeforeEach
  void setUp() {
    testQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Test Query")
            .category("Testing")
            .connectionName("test-db")
            .queryType(QueryType.SELECT)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("testuser")
            .build();
    queryRepository.save(testQuery);
  }

  @Test
  void shouldSaveAndFindExecutionLog() {
    ExecutionLog log =
        ExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .queryId(testQuery.getId())
            .queryVersion(1)
            .connectionName("test-db")
            .executedBy("testuser")
            .executedAt(LocalDateTime.now())
            .rowCount(10)
            .executionTimeMs(100L)
            .status(ExecutionStatus.SUCCESS)
            .executionType(ExecutionType.SELECT)
            .build();

    ExecutionLog saved = executionLogRepository.save(log);

    assertThat(saved.getId()).isNotNull();
    assertThat(executionLogRepository.findById(saved.getId())).isPresent();
  }

  @Test
  void shouldFindByExecutedBy() {
    ExecutionLog log =
        ExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .queryId(testQuery.getId())
            .queryVersion(1)
            .connectionName("test-db")
            .executedBy("testuser")
            .executedAt(LocalDateTime.now())
            .status(ExecutionStatus.SUCCESS)
            .executionType(ExecutionType.SELECT)
            .build();
    executionLogRepository.save(log);

    Page<ExecutionLog> logs =
        executionLogRepository.findByExecutedByOrderByExecutedAtDesc(
            "testuser", PageRequest.of(0, 10));

    assertThat(logs.getTotalElements()).isEqualTo(1);
  }

  @Test
  void shouldFindWithFilters() {
    ExecutionLog log1 =
        ExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .queryId(testQuery.getId())
            .queryVersion(1)
            .connectionName("test-db")
            .executedBy("user1")
            .executedAt(LocalDateTime.now())
            .status(ExecutionStatus.SUCCESS)
            .executionType(ExecutionType.SELECT)
            .build();
    executionLogRepository.save(log1);

    ExecutionLog log2 =
        ExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .queryId(testQuery.getId())
            .queryVersion(1)
            .connectionName("test-db")
            .executedBy("user2")
            .executedAt(LocalDateTime.now())
            .status(ExecutionStatus.FAILED)
            .executionType(ExecutionType.SELECT)
            .build();
    executionLogRepository.save(log2);

    Page<ExecutionLog> successLogs =
        executionLogRepository.findWithFilters(
            null, null, ExecutionStatus.SUCCESS, null, null, null, PageRequest.of(0, 10));

    assertThat(successLogs.getTotalElements()).isEqualTo(1);
    assertThat(successLogs.getContent().get(0).getExecutedBy()).isEqualTo("user1");
  }

  @Test
  void shouldCountByStatus() {
    ExecutionLog successLog =
        ExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .queryId(testQuery.getId())
            .queryVersion(1)
            .connectionName("test-db")
            .executedBy("testuser")
            .executedAt(LocalDateTime.now())
            .status(ExecutionStatus.SUCCESS)
            .executionType(ExecutionType.SELECT)
            .build();
    executionLogRepository.save(successLog);

    ExecutionLog failedLog =
        ExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .queryId(testQuery.getId())
            .queryVersion(1)
            .connectionName("test-db")
            .executedBy("testuser")
            .executedAt(LocalDateTime.now())
            .status(ExecutionStatus.FAILED)
            .executionType(ExecutionType.SELECT)
            .build();
    executionLogRepository.save(failedLog);

    assertThat(executionLogRepository.countByStatus(ExecutionStatus.SUCCESS)).isEqualTo(1);
    assertThat(executionLogRepository.countByStatus(ExecutionStatus.FAILED)).isEqualTo(1);
  }
}
