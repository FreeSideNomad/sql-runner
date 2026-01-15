package com.ivamare.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Tests for QueryDto. */
class QueryDtoTest {

  @Test
  void from_createsCorrectDto() {
    LocalDateTime now = LocalDateTime.now();
    Query query =
        Query.builder()
            .id("test-id")
            .name("Test Query")
            .description("Description")
            .category("Test Category")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .currentVersion(2)
            .createdAt(now)
            .createdBy("admin")
            .updatedAt(now.plusHours(1))
            .updatedBy("editor")
            .build();

    QueryDto dto = QueryDto.from(query);

    assertThat(dto.getId()).isEqualTo("test-id");
    assertThat(dto.getName()).isEqualTo("Test Query");
    assertThat(dto.getDescription()).isEqualTo("Description");
    assertThat(dto.getCategory()).isEqualTo("Test Category");
    assertThat(dto.getConnectionName()).isEqualTo("test-conn");
    assertThat(dto.getQueryType()).isEqualTo(QueryType.SELECT);
    assertThat(dto.getCurrentVersion()).isEqualTo(2);
    assertThat(dto.getCreatedAt()).isEqualTo(now);
    assertThat(dto.getCreatedBy()).isEqualTo("admin");
    assertThat(dto.getUpdatedAt()).isEqualTo(now.plusHours(1));
    assertThat(dto.getUpdatedBy()).isEqualTo("editor");
  }

  @Test
  void from_handlesNullUpdatedFields() {
    Query query =
        Query.builder()
            .id("test-id")
            .name("Test Query")
            .category("Test Category")
            .connectionName("test-conn")
            .queryType(QueryType.UPDATE_WORKFLOW)
            .currentVersion(1)
            .createdAt(LocalDateTime.now())
            .createdBy("admin")
            .build();

    QueryDto dto = QueryDto.from(query);

    assertThat(dto.getUpdatedAt()).isNull();
    assertThat(dto.getUpdatedBy()).isNull();
  }
}
