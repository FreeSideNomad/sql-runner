package com.ivamare.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class QueryRepositoryTest {

  @Autowired private QueryRepository queryRepository;

  private Query testQuery;

  @BeforeEach
  void setUp() {
    testQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Test Query")
            .description("A test query")
            .category("Testing")
            .connectionName("test-db")
            .queryType(QueryType.SELECT)
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("testuser")
            .build();
  }

  @Test
  void shouldSaveAndFindQuery() {
    Query saved = queryRepository.save(testQuery);

    assertThat(saved.getId()).isNotNull();
    assertThat(queryRepository.findById(saved.getId())).isPresent();
  }

  @Test
  void shouldFindByCategory() {
    queryRepository.save(testQuery);

    List<Query> queries = queryRepository.findByCategory("Testing");

    assertThat(queries).hasSize(1);
    assertThat(queries.get(0).getName()).isEqualTo("Test Query");
  }

  @Test
  void shouldFindActiveQueries() {
    queryRepository.save(testQuery);

    Query inactiveQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Inactive Query")
            .category("Testing")
            .connectionName("test-db")
            .queryType(QueryType.SELECT)
            .isActive(false)
            .createdAt(LocalDateTime.now())
            .createdBy("testuser")
            .build();
    queryRepository.save(inactiveQuery);

    List<Query> activeQueries = queryRepository.findByIsActiveTrue();

    assertThat(activeQueries).hasSize(1);
    assertThat(activeQueries.get(0).getName()).isEqualTo("Test Query");
  }

  @Test
  void shouldFindDistinctCategories() {
    queryRepository.save(testQuery);

    Query anotherQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Another Query")
            .category("Reports")
            .connectionName("test-db")
            .queryType(QueryType.SELECT)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("testuser")
            .build();
    queryRepository.save(anotherQuery);

    List<String> categories = queryRepository.findDistinctCategories();

    assertThat(categories).containsExactly("Reports", "Testing");
  }

  @Test
  void shouldFindByQueryType() {
    queryRepository.save(testQuery);

    Query updateQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Update Query")
            .category("Testing")
            .connectionName("test-db")
            .queryType(QueryType.UPDATE_WORKFLOW)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("testuser")
            .build();
    queryRepository.save(updateQuery);

    List<Query> selectQueries = queryRepository.findByQueryTypeAndIsActiveTrue(QueryType.SELECT);
    List<Query> updateQueries =
        queryRepository.findByQueryTypeAndIsActiveTrue(QueryType.UPDATE_WORKFLOW);

    assertThat(selectQueries).hasSize(1);
    assertThat(updateQueries).hasSize(1);
  }
}
