package com.ivamare.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
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

    QueryConfig parsedConfig = QueryConfig.builder().sql("SELECT 1").build();

    QueryFormDto dto = QueryFormDto.from(query, parsedConfig);

    assertThat(dto.getId()).isEqualTo("test-id");
    assertThat(dto.getName()).isEqualTo("Test Query");
    assertThat(dto.getDescription()).isEqualTo("Description");
    assertThat(dto.getCategory()).isEqualTo("Test Category");
    assertThat(dto.getConnectionName()).isEqualTo("test-conn");
    assertThat(dto.getQueryType()).isEqualTo(QueryType.SELECT);
    assertThat(dto.getConfig()).isNotNull();
    assertThat(dto.getConfig().getSql()).isEqualTo("SELECT 1");
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

  @Test
  void ensureConfigInitialized_createsEmptyConfigAndParameters() {
    QueryFormDto dto = QueryFormDto.builder().build();

    dto.ensureConfigInitialized();

    assertThat(dto.getConfig()).isNotNull();
    assertThat(dto.getConfig().getParameters()).isNotNull();
    assertThat(dto.getConfig().getParameters()).isEmpty();
  }
}
