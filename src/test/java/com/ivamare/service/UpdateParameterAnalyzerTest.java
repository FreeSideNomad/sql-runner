package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.UpdateBindingMode;
import com.ivamare.dto.QueryConfig;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UpdateParameterAnalyzerTest {

  private UpdateParameterAnalyzer analyzer;

  @BeforeEach
  void setUp() {
    analyzer = new UpdateParameterAnalyzer();
  }

  @Nested
  class ExtractParameters {

    @Test
    void returnsEmptySet_forNullSql() {
      assertThat(analyzer.extractParameters(null)).isEmpty();
    }

    @Test
    void returnsEmptySet_forBlankSql() {
      assertThat(analyzer.extractParameters("   ")).isEmpty();
    }

    @Test
    void returnsEmptySet_forSqlWithoutParams() {
      assertThat(analyzer.extractParameters("SELECT * FROM test")).isEmpty();
    }

    @Test
    void extractsSingleParameter() {
      Set<String> params = analyzer.extractParameters("SELECT * FROM test WHERE id = :id");
      assertThat(params).containsExactly("id");
    }

    @Test
    void extractsMultipleParameters() {
      Set<String> params =
          analyzer.extractParameters(
              "UPDATE test SET name = :name, status = :status WHERE id = :id");
      assertThat(params).containsExactlyInAnyOrder("name", "status", "id");
    }

    @Test
    void extractsParametersLowercase() {
      Set<String> params = analyzer.extractParameters("SELECT * FROM test WHERE ID = :ID");
      assertThat(params).containsExactly("id");
    }

    @Test
    void extractsIdListParameter() {
      Set<String> params =
          analyzer.extractParameters("UPDATE test SET status = :status WHERE id IN (:id_list)");
      assertThat(params).containsExactlyInAnyOrder("status", "id_list");
    }
  }

  @Nested
  class ExtractSelectColumns {

    @Test
    void returnsEmptySet_forNullSql() {
      assertThat(analyzer.extractSelectColumns(null)).isEmpty();
    }

    @Test
    void returnsEmptySet_forBlankSql() {
      assertThat(analyzer.extractSelectColumns("   ")).isEmpty();
    }

    @Test
    void returnsEmptySet_forSelectStar() {
      assertThat(analyzer.extractSelectColumns("SELECT * FROM test")).isEmpty();
    }

    @Test
    void returnsEmptySet_forInvalidSql() {
      assertThat(analyzer.extractSelectColumns("UPDATE test SET name = 'foo'")).isEmpty();
    }

    @Test
    void extractsSingleColumn() {
      Set<String> columns = analyzer.extractSelectColumns("SELECT id FROM test");
      assertThat(columns).containsExactly("id");
    }

    @Test
    void extractsMultipleColumns() {
      Set<String> columns = analyzer.extractSelectColumns("SELECT id, name, status FROM test");
      assertThat(columns).containsExactlyInAnyOrder("id", "name", "status");
    }

    @Test
    void extractsColumnsLowercase() {
      Set<String> columns = analyzer.extractSelectColumns("SELECT ID, NAME FROM test");
      assertThat(columns).containsExactlyInAnyOrder("id", "name");
    }

    @Test
    void extractsColumnFromTableQualified() {
      Set<String> columns = analyzer.extractSelectColumns("SELECT t.id, t.name FROM test t");
      assertThat(columns).containsExactlyInAnyOrder("id", "name");
    }

    @Test
    void extractsAliasFromAsExpression() {
      Set<String> columns =
          analyzer.extractSelectColumns("SELECT id, first_name AS name FROM test");
      assertThat(columns).containsExactlyInAnyOrder("id", "name");
    }

    @Test
    void extractsAliasFromSpaceExpression() {
      Set<String> columns = analyzer.extractSelectColumns("SELECT id, first_name name FROM test");
      assertThat(columns).containsExactlyInAnyOrder("id", "name");
    }
  }

  @Nested
  class DetectBindingMode {

    @Test
    void returnsBatch_whenIdListPresent() {
      UpdateBindingMode mode =
          analyzer.detectBindingMode(
              "UPDATE test SET status = :status WHERE id IN (:id_list)", Set.of("id", "name"));
      assertThat(mode).isEqualTo(UpdateBindingMode.BATCH);
    }

    @Test
    void returnsRowByRow_whenColumnParamPresent() {
      UpdateBindingMode mode =
          analyzer.detectBindingMode(
              "UPDATE test SET name = UPPER(:name) WHERE id = :id", Set.of("id", "name"));
      assertThat(mode).isEqualTo(UpdateBindingMode.ROW_BY_ROW);
    }

    @Test
    void returnsStandard_whenOnlyUserParams() {
      UpdateBindingMode mode =
          analyzer.detectBindingMode(
              "UPDATE test SET status = :newStatus WHERE region = :region", Set.of("id", "name"));
      assertThat(mode).isEqualTo(UpdateBindingMode.STANDARD);
    }

    @Test
    void returnsBatch_evenWhenColumnParamsAlsoPresent() {
      // :id_list takes precedence
      UpdateBindingMode mode =
          analyzer.detectBindingMode(
              "UPDATE test SET name = :name WHERE id IN (:id_list)", Set.of("id", "name"));
      assertThat(mode).isEqualTo(UpdateBindingMode.BATCH);
    }

    @Test
    void returnsStandard_forEmptySelectColumns() {
      UpdateBindingMode mode =
          analyzer.detectBindingMode("UPDATE test SET status = :status WHERE id = :id", Set.of());
      assertThat(mode).isEqualTo(UpdateBindingMode.STANDARD);
    }
  }

  @Nested
  class AnalyzeBindings {

    @Test
    void separatesColumnAndUserParams() {
      List<QueryConfig.ParameterConfig> configParams =
          List.of(
              QueryConfig.ParameterConfig.builder().name("newStatus").build(),
              QueryConfig.ParameterConfig.builder().name("region").build());

      UpdateParameterAnalyzer.ParameterBindings bindings =
          analyzer.analyzeBindings(
              "UPDATE test SET status = :newStatus, name = UPPER(:name) WHERE id = :id",
              Set.of("id", "name", "email"),
              configParams);

      assertThat(bindings.getColumnBoundParams()).containsExactlyInAnyOrder("id", "name");
      assertThat(bindings.getUserBoundParams()).containsExactly("newstatus");
    }

    @Test
    void excludesIdListFromBothSets() {
      List<QueryConfig.ParameterConfig> configParams =
          List.of(QueryConfig.ParameterConfig.builder().name("status").build());

      UpdateParameterAnalyzer.ParameterBindings bindings =
          analyzer.analyzeBindings(
              "UPDATE test SET status = :status WHERE id IN (:id_list)",
              Set.of("id", "name"),
              configParams);

      assertThat(bindings.getColumnBoundParams()).isEmpty();
      assertThat(bindings.getUserBoundParams()).containsExactly("status");
    }

    @Test
    void handlesNullConfigParams() {
      UpdateParameterAnalyzer.ParameterBindings bindings =
          analyzer.analyzeBindings(
              "UPDATE test SET name = :name WHERE id = :id", Set.of("id", "name"), null);

      assertThat(bindings.getColumnBoundParams()).containsExactlyInAnyOrder("id", "name");
      assertThat(bindings.getUserBoundParams()).isEmpty();
    }

    @Test
    void caseInsensitiveColumnMatching() {
      UpdateParameterAnalyzer.ParameterBindings bindings =
          analyzer.analyzeBindings(
              "UPDATE test SET NAME = :NAME WHERE ID = :ID", Set.of("ID", "NAME"), null);

      assertThat(bindings.getColumnBoundParams()).containsExactlyInAnyOrder("id", "name");
    }
  }
}
