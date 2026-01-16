package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ivamare.domain.DatabaseType;
import com.ivamare.domain.UpdateBindingMode;
import com.ivamare.dto.QueryConfig;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlScriptGeneratorTest {

  private UpdateParameterAnalyzer parameterAnalyzer;
  private SqlScriptGenerator generator;

  @BeforeEach
  void setUp() {
    parameterAnalyzer = mock(UpdateParameterAnalyzer.class);
    generator = new SqlScriptGenerator(parameterAnalyzer);
  }

  @Test
  void generateScript_standardMode_generatesCorrectSql() {
    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.STANDARD)
            .updateSql("UPDATE test SET val = :val WHERE id = :id")
            .build();

    Map<String, Object> params = Map.of("val", "new_value", "id", 123);

    String script =
        generator.generateScript(config, params, Collections.emptyList(), DatabaseType.SQLSERVER);

    assertThat(script).contains("BEGIN TRANSACTION");
    assertThat(script).contains("UPDATE test SET val = 'new_value' WHERE id = 123");
    assertThat(script).contains("COMMIT TRANSACTION");
  }

  @Test
  void generateScript_standardMode_replacesParams_caseInsensitive() {
    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.STANDARD)
            .updateSql("UPDATE test SET VAL = :VAL WHERE ID = :ID")
            .build();

    Map<String, Object> params = Map.of("val", "new_value", "id", 123);

    String script =
        generator.generateScript(config, params, Collections.emptyList(), DatabaseType.SQLSERVER);

    assertThat(script).contains("UPDATE test SET VAL = 'new_value' WHERE ID = 123");
  }

  @Test
  void generateScript_batchMode_generatesCorrectSql() {
    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.BATCH)
            .updateSql("UPDATE test SET val = :val WHERE id IN (:id_list)")
            .primaryKeyColumn("id")
            .build();

    Map<String, Object> params = Map.of("val", "batch_val");
    List<Map<String, Object>> previewData =
        List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3));

    String script = generator.generateScript(config, params, previewData, DatabaseType.POSTGRES);

    assertThat(script).contains("BEGIN;");
    assertThat(script).contains("UPDATE test SET val = 'batch_val' WHERE id IN (1, 2, 3)");
    assertThat(script).contains("COMMIT;");
  }

  @Test
  void generateScript_rowByRowMode_generatesCorrectSql() {
    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.ROW_BY_ROW)
            .updateSql("UPDATE test SET val = :val WHERE id = :id")
            .parameters(List.of())
            .build();

    Map<String, Object> params = Map.of("val", "row_val");
    List<Map<String, Object>> previewData = List.of(Map.of("id", 10), Map.of("id", 20));

    when(parameterAnalyzer.analyzeBindings(any(), any(), any()))
        .thenReturn(new UpdateParameterAnalyzer.ParameterBindings(Set.of("id"), Set.of("val")));

    String script = generator.generateScript(config, params, previewData, DatabaseType.H2);

    assertThat(script).contains("UPDATE test SET val = 'row_val' WHERE id = 10");
    assertThat(script).contains("UPDATE test SET val = 'row_val' WHERE id = 20");
    assertThat(script).contains("COMMIT;");
  }

  @Test
  void generateScript_postgres_generatesCorrectTransactionControl() {

    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.STANDARD)
            .updateSql("UPDATE test SET val = 1")
            .build();

    String script =
        generator.generateScript(
            config, Collections.emptyMap(), Collections.emptyList(), DatabaseType.POSTGRES);

    assertThat(script).contains("BEGIN;");

    assertThat(script).contains("COMMIT;");

    assertThat(script).contains("-- Note: In case of error, execute ROLLBACK;");
  }

  @Test
  void generateScript_db2_generatesCorrectTransactionControl() {

    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.STANDARD)
            .updateSql("UPDATE test SET val = 1")
            .build();

    String script =
        generator.generateScript(
            config, Collections.emptyMap(), Collections.emptyList(), DatabaseType.DB2);

    assertThat(script).contains("BEGIN;");

    assertThat(script).contains("COMMIT;");
  }

  @Test
  void generateScript_h2_generatesCorrectTransactionControl() {

    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.STANDARD)
            .updateSql("UPDATE test SET val = 1")
            .build();

    String script =
        generator.generateScript(
            config, Collections.emptyMap(), Collections.emptyList(), DatabaseType.H2);

    assertThat(script).contains("-- Transaction start");

    assertThat(script).contains("COMMIT;");
  }

  @Test
  void generateScript_sqlServer_generatesCorrectTransactionControl() {

    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.STANDARD)
            .updateSql("UPDATE test SET val = 1")
            .build();

    String script =
        generator.generateScript(
            config, Collections.emptyMap(), Collections.emptyList(), DatabaseType.SQLSERVER);

    assertThat(script).contains("BEGIN TRY");

    assertThat(script).contains("BEGIN TRANSACTION;");

    assertThat(script).contains("COMMIT TRANSACTION;");

    assertThat(script).contains("END TRY");

    assertThat(script).contains("BEGIN CATCH");

    assertThat(script).contains("ROLLBACK TRANSACTION;");

    assertThat(script).contains("RAISERROR");
  }

  @Test
  void generateScript_batchMode_emptyPreview_generatesComment() {

    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.BATCH)
            .updateSql("UPDATE test SET val = :val WHERE id IN (:id_list)")
            .primaryKeyColumn("id")
            .build();

    String script =
        generator.generateScript(
            config, Map.of("val", 1), Collections.emptyList(), DatabaseType.POSTGRES);

    assertThat(script).contains("-- No rows to update");
  }

  @Test
  void generateScript_handlesBooleanTypes() {

    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.STANDARD)
            .updateSql("INSERT INTO test VALUES (:p1, :p2)")
            .build();

    Map<String, Object> params = Map.of("p1", true, "p2", false);

    // SQL Server uses 1/0

    String sqlServerScript =
        generator.generateScript(config, params, Collections.emptyList(), DatabaseType.SQLSERVER);

    assertThat(sqlServerScript).contains("VALUES (1, 0)");

    // Postgres uses TRUE/FALSE (represented as string here since formatValue .toString())

    // Actually current impl:

    // if (value instanceof Boolean) {

    //   if (dbType == SQLSERVER || dbType == DB2) return "1"/"0";

    //   return value.toString().toUpperCase(); -> "TRUE"/"FALSE"

    // }

    String postgresScript =
        generator.generateScript(config, params, Collections.emptyList(), DatabaseType.POSTGRES);

    assertThat(postgresScript).contains("VALUES (TRUE, FALSE)");
  }

  @Test
  void generateScript_handlesListTypes() {

    QueryConfig config =
        QueryConfig.builder()
            .updateBindingMode(UpdateBindingMode.STANDARD)
            .updateSql("SELECT * FROM test WHERE id IN (:ids)")
            .build();

    Map<String, Object> params = Map.of("ids", List.of(1, 2, 3));

    String script =
        generator.generateScript(config, params, Collections.emptyList(), DatabaseType.POSTGRES);

    assertThat(script).contains("IN (1, 2, 3)");
  }

  private static <T> T any() {

    return org.mockito.ArgumentMatchers.any();
  }
}
