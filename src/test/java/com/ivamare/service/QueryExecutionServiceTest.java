package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivamare.dto.QueryConfig;
import com.ivamare.dto.QueryConfig.ParameterConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Tests for QueryExecutionService. */
@SpringBootTest
@ActiveProfiles("test")
class QueryExecutionServiceTest {

  @Autowired private QueryExecutionService executionService;

  @Test
  void parseConfig_parsesSimpleSelectQuery() {
    String yaml =
        """
        sql: SELECT * FROM customers WHERE region = :region
        parameters:
          - name: region
            label: Region
            dataType: STRING
            required: true
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getSql()).isEqualTo("SELECT * FROM customers WHERE region = :region");
    assertThat(config.getParameters()).hasSize(1);
    assertThat(config.getParameters().get(0).getName()).isEqualTo("region");
    assertThat(config.getParameters().get(0).getLabel()).isEqualTo("Region");
    assertThat(config.getParameters().get(0).getDataType()).isEqualTo("STRING");
    assertThat(config.getParameters().get(0).isRequired()).isTrue();
  }

  @Test
  void parseConfig_parsesUpdateWorkflow() {
    String yaml =
        """
        selectSql: SELECT id, name FROM customers WHERE status = :status
        updateSql: UPDATE customers SET status = :newStatus WHERE status = :status
        primaryKeyColumn: id
        backupColumns:
          - name
          - status
        rollbackColumns:
          - status
        parameters:
          - name: status
            dataType: STRING
            required: true
          - name: newStatus
            dataType: STRING
            required: true
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getSelectSql()).contains("SELECT id, name");
    assertThat(config.getUpdateSql()).contains("UPDATE customers");
    assertThat(config.getPrimaryKeyColumn()).isEqualTo("id");
    assertThat(config.getBackupColumns()).containsExactly("name", "status");
    assertThat(config.getRollbackColumns()).containsExactly("status");
  }

  @Test
  void parseConfig_parsesEnumParameter() {
    String yaml =
        """
        sql: SELECT * FROM customers WHERE status = :status
        parameters:
          - name: status
            dataType: ENUM
            required: true
            enumValues:
              - value: A
                description: Active
              - value: I
                description: Inactive
        """;

    QueryConfig config = executionService.parseConfig(yaml);
    ParameterConfig param = config.getParameters().get(0);

    assertThat(param.getEnumValues()).hasSize(2);
    assertThat(param.getEnumValues().get(0).getValue()).isEqualTo("A");
    assertThat(param.getEnumValues().get(0).getDescription()).isEqualTo("Active");
  }

  @Test
  void convertParameters_convertsStringType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("region").dataType("STRING").build());
    Map<String, String> raw = Map.of("region", "EAST");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("region")).isEqualTo("EAST");
  }

  @Test
  void convertParameters_convertsIntegerType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("count").dataType("INTEGER").build());
    Map<String, String> raw = Map.of("count", "42");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("count")).isEqualTo(42);
  }

  @Test
  void convertParameters_convertsDecimalType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("amount").dataType("DECIMAL").build());
    Map<String, String> raw = Map.of("amount", "123.45");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("amount")).isEqualTo(new BigDecimal("123.45"));
  }

  @Test
  void convertParameters_convertsDateType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("startDate").dataType("DATE").build());
    Map<String, String> raw = Map.of("startDate", "2024-01-15");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("startDate")).isEqualTo(LocalDate.of(2024, 1, 15));
  }

  @Test
  void convertParameters_convertsBooleanType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("active").dataType("BOOLEAN").build());
    Map<String, String> raw = Map.of("active", "true");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("active")).isEqualTo(true);
  }

  @Test
  void convertParameters_convertsListStringType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("ids").dataType("LIST_STRING").build());
    Map<String, String> raw = Map.of("ids", "ABC\nDEF,GHI");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) converted.get("ids");
    assertThat(list).containsExactly("ABC", "DEF", "GHI");
  }

  @Test
  void convertParameters_convertsListIntegerType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("ids").dataType("LIST_INTEGER").build());
    Map<String, String> raw = Map.of("ids", "1,2,3");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    @SuppressWarnings("unchecked")
    List<Integer> list = (List<Integer>) converted.get("ids");
    assertThat(list).containsExactly(1, 2, 3);
  }

  @Test
  void convertParameters_usesDefaultValue() {
    List<ParameterConfig> configs =
        List.of(
            ParameterConfig.builder()
                .name("region")
                .dataType("STRING")
                .defaultValue("WEST")
                .build());
    Map<String, String> raw = new HashMap<>();

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("region")).isEqualTo("WEST");
  }

  @Test
  void convertParameters_throwsForMissingRequiredParam() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("region").dataType("STRING").required(true).build());
    Map<String, String> raw = new HashMap<>();

    assertThatThrownBy(() -> executionService.convertParameters(raw, configs))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Required parameter missing: region");
  }

  @Test
  void getExecutableSql_returnsSqlForSelectQuery() {
    QueryConfig config = QueryConfig.builder().sql("SELECT * FROM test").build();

    assertThat(config.getExecutableSql()).isEqualTo("SELECT * FROM test");
  }

  @Test
  void getExecutableSql_returnsSelectSqlForUpdateWorkflow() {
    QueryConfig config = QueryConfig.builder().selectSql("SELECT * FROM test").build();

    assertThat(config.getExecutableSql()).isEqualTo("SELECT * FROM test");
  }

  @Test
  void convertParameters_convertsDateTimeType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("timestamp").dataType("DATETIME").build());
    Map<String, String> raw = Map.of("timestamp", "2024-01-15T10:30:00");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("timestamp"))
        .isEqualTo(java.time.LocalDateTime.of(2024, 1, 15, 10, 30, 0));
  }

  @Test
  void convertParameters_convertsEnumType() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("status").dataType("ENUM").build());
    Map<String, String> raw = Map.of("status", "ACTIVE");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("status")).isEqualTo("ACTIVE");
  }

  @Test
  void convertParameters_withNullConfigs_returnsEmptyMap() {
    Map<String, String> raw = Map.of("foo", "bar");

    Map<String, Object> converted = executionService.convertParameters(raw, null);

    assertThat(converted).isEmpty();
  }

  @Test
  void convertParameters_withOptionalAndEmpty_setsNull() {
    List<ParameterConfig> configs =
        List.of(
            ParameterConfig.builder().name("optional").dataType("STRING").required(false).build());
    Map<String, String> raw = Map.of("optional", "");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("optional")).isNull();
  }

  @Test
  void convertParameters_withListStringNewlineSeparator() {
    List<ParameterConfig> configs =
        List.of(
            ParameterConfig.builder()
                .name("ids")
                .dataType("LIST_STRING")
                .listSeparator("NEWLINE")
                .build());
    Map<String, String> raw = Map.of("ids", "ABC\nDEF\nGHI");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) converted.get("ids");
    assertThat(list).containsExactly("ABC", "DEF", "GHI");
  }

  @Test
  void convertParameters_withListStringCommaSeparator() {
    List<ParameterConfig> configs =
        List.of(
            ParameterConfig.builder()
                .name("ids")
                .dataType("LIST_STRING")
                .listSeparator("COMMA")
                .build());
    Map<String, String> raw = Map.of("ids", "ABC,DEF,GHI");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) converted.get("ids");
    assertThat(list).containsExactly("ABC", "DEF", "GHI");
  }

  @Test
  void convertParameters_withUnknownType_returnsRawValue() {
    List<ParameterConfig> configs =
        List.of(ParameterConfig.builder().name("unknown").dataType("UNKNOWN_TYPE").build());
    Map<String, String> raw = Map.of("unknown", "value123");

    Map<String, Object> converted = executionService.convertParameters(raw, configs);

    assertThat(converted.get("unknown")).isEqualTo("value123");
  }

  @Test
  void parseConfig_withTimeoutAndMaxRows() {
    String yaml =
        """
        sql: SELECT * FROM test
        timeoutSeconds: 30
        maxRows: 1000
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getTimeoutSeconds()).isEqualTo(30);
    assertThat(config.getMaxRows()).isEqualTo(1000);
  }

  @Test
  void parseConfig_withoutParameters_returnsEmptyList() {
    String yaml = """
        sql: SELECT * FROM test
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getParameters()).isEmpty();
  }

  @Test
  void parseConfig_parsesUpdateBindingModeBatch() {
    String yaml =
        """
        selectSql: SELECT id FROM test
        updateSql: UPDATE test SET x = 1 WHERE id IN (:id_list)
        updateBindingMode: BATCH
        primaryKeyColumn: id
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getUpdateBindingMode()).isEqualTo(com.ivamare.domain.UpdateBindingMode.BATCH);
  }

  @Test
  void parseConfig_parsesUpdateBindingModeRowByRow() {
    String yaml =
        """
        selectSql: SELECT id, name FROM test
        updateSql: UPDATE test SET name = UPPER(:name) WHERE id = :id
        updateBindingMode: ROW_BY_ROW
        primaryKeyColumn: id
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getUpdateBindingMode())
        .isEqualTo(com.ivamare.domain.UpdateBindingMode.ROW_BY_ROW);
  }

  @Test
  void parseConfig_parsesUpdateBindingModeStandard() {
    String yaml =
        """
        selectSql: SELECT * FROM test
        updateSql: UPDATE test SET x = :x
        updateBindingMode: STANDARD
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getUpdateBindingMode())
        .isEqualTo(com.ivamare.domain.UpdateBindingMode.STANDARD);
  }

  @Test
  void parseConfig_invalidUpdateBindingMode_returnsNull() {
    String yaml =
        """
        selectSql: SELECT * FROM test
        updateSql: UPDATE test SET x = :x
        updateBindingMode: INVALID_MODE
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getUpdateBindingMode()).isNull();
  }

  @Test
  void parseConfig_withoutUpdateBindingMode_returnsNull() {
    String yaml =
        """
        selectSql: SELECT * FROM test
        updateSql: UPDATE test SET x = :x
        """;

    QueryConfig config = executionService.parseConfig(yaml);

    assertThat(config.getUpdateBindingMode()).isNull();
  }
}
