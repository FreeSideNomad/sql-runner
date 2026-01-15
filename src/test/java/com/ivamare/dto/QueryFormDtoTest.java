package com.ivamare.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.domain.QueryVersion;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Tests for QueryFormDto. */
class QueryFormDtoTest {

  @Test
  void from_createsCorrectFormDto() {
    Query query =
        Query.builder()
            .id("test-id")
            .name("Test Query")
            .description("Description")
            .category("Test Category")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .build();

    QueryVersion version =
        QueryVersion.builder()
            .id("version-id")
            .query(query)
            .version(1)
            .configYaml("sql: SELECT 1")
            .createdAt(LocalDateTime.now())
            .createdBy("admin")
            .build();

    QueryFormDto dto = QueryFormDto.from(query, version);

    assertThat(dto.getId()).isEqualTo("test-id");
    assertThat(dto.getName()).isEqualTo("Test Query");
    assertThat(dto.getDescription()).isEqualTo("Description");
    assertThat(dto.getCategory()).isEqualTo("Test Category");
    assertThat(dto.getConnectionName()).isEqualTo("test-conn");
    assertThat(dto.getQueryType()).isEqualTo(QueryType.SELECT);
    assertThat(dto.getConfigYaml()).isEqualTo("sql: SELECT 1");
  }

  @Test
  void isEdit_returnsTrueWhenIdPresent() {
    QueryFormDto dto = QueryFormDto.builder().id("existing-id").build();

    assertThat(dto.isEdit()).isTrue();
  }

  @Test
  void isEdit_returnsFalseWhenIdNull() {
    QueryFormDto dto = QueryFormDto.builder().id(null).build();

    assertThat(dto.isEdit()).isFalse();
  }

  @Test
  void isEdit_returnsFalseWhenIdEmpty() {
    QueryFormDto dto = QueryFormDto.builder().id("").build();

    assertThat(dto.isEdit()).isFalse();
  }
}
