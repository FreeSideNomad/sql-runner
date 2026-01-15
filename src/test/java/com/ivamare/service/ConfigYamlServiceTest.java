package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivamare.dto.QueryConfig;
import com.ivamare.dto.QueryConfig.EnumValue;
import com.ivamare.dto.QueryConfig.ParameterConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for ConfigYamlService. */
class ConfigYamlServiceTest {

  private final ConfigYamlService service = new ConfigYamlService();

  @Test
  void parse_withNullYaml_returnsEmptyConfig() {
    QueryConfig result = service.parse(null);
    assertThat(result).isNotNull();
  }

  @Test
  void parse_withBlankYaml_returnsEmptyConfig() {
    QueryConfig result = service.parse("   ");
    assertThat(result).isNotNull();
  }

  @Test
  void parse_withValidSelectQueryYaml_returnsConfig() {
    String yaml =
        """
        sql: SELECT * FROM customers WHERE region = :region
        parameters:
          - name: region
            label: Region
            dataType: STRING
            required: true
        """;

    QueryConfig result = service.parse(yaml);

    assertThat(result.getSql()).isEqualTo("SELECT * FROM customers WHERE region = :region");
    assertThat(result.getParameters()).hasSize(1);
    assertThat(result.getParameters().get(0).getName()).isEqualTo("region");
    assertThat(result.getParameters().get(0).getLabel()).isEqualTo("Region");
    assertThat(result.getParameters().get(0).getDataType()).isEqualTo("STRING");
    assertThat(result.getParameters().get(0).isRequired()).isTrue();
  }

  @Test
  void parse_withUpdateWorkflowYaml_returnsConfig() {
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
        """;

    QueryConfig result = service.parse(yaml);

    assertThat(result.getSelectSql()).contains("SELECT id, name");
    assertThat(result.getUpdateSql()).contains("UPDATE customers");
    assertThat(result.getPrimaryKeyColumn()).isEqualTo("id");
    assertThat(result.getBackupColumns()).containsExactly("name", "status");
    assertThat(result.getRollbackColumns()).containsExactly("status");
  }

  @Test
  void parse_withOptionalSettings_returnsConfig() {
    String yaml =
        """
        sql: SELECT * FROM test
        timeoutSeconds: 120
        maxRows: 500
        """;

    QueryConfig result = service.parse(yaml);

    assertThat(result.getTimeoutSeconds()).isEqualTo(120);
    assertThat(result.getMaxRows()).isEqualTo(500);
  }

  @Test
  void parse_withEnumParameter_returnsConfig() {
    String yaml =
        """
        sql: SELECT * FROM test WHERE status = :status
        parameters:
          - name: status
            dataType: ENUM
            enumValues:
              - value: A
                description: Active
              - value: I
                description: Inactive
        """;

    QueryConfig result = service.parse(yaml);

    assertThat(result.getParameters()).hasSize(1);
    ParameterConfig param = result.getParameters().get(0);
    assertThat(param.getEnumValues()).hasSize(2);
    assertThat(param.getEnumValues().get(0).getValue()).isEqualTo("A");
    assertThat(param.getEnumValues().get(0).getDescription()).isEqualTo("Active");
  }

  @Test
  void parse_withInvalidYaml_throwsException() {
    String yaml = "invalid: [yaml";

    assertThatThrownBy(() -> service.parse(yaml))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid config YAML");
  }

  @Test
  void toYaml_withNullConfig_returnsEmptyString() {
    String result = service.toYaml(null);
    assertThat(result).isEmpty();
  }

  @Test
  void toYaml_withSelectQuery_returnsYaml() {
    QueryConfig config =
        QueryConfig.builder()
            .sql("SELECT * FROM customers")
            .parameters(
                List.of(
                    ParameterConfig.builder()
                        .name("region")
                        .label("Region")
                        .dataType("STRING")
                        .required(true)
                        .build()))
            .build();

    String result = service.toYaml(config);

    assertThat(result).contains("sql: SELECT * FROM customers");
    assertThat(result).contains("name: region");
    assertThat(result).contains("label: Region");
    assertThat(result).contains("dataType: STRING");
    assertThat(result).contains("required: true");
  }

  @Test
  void toYaml_withUpdateWorkflow_returnsYaml() {
    QueryConfig config =
        QueryConfig.builder()
            .selectSql("SELECT id FROM test")
            .updateSql("UPDATE test SET col = :val")
            .primaryKeyColumn("id")
            .backupColumns(List.of("col1", "col2"))
            .rollbackColumns(List.of("col1"))
            .build();

    String result = service.toYaml(config);

    assertThat(result).contains("selectSql:");
    assertThat(result).contains("updateSql:");
    assertThat(result).contains("primaryKeyColumn: id");
    assertThat(result).contains("backupColumns:");
    assertThat(result).contains("rollbackColumns:");
  }

  @Test
  void toYaml_withOptionalSettings_includesThem() {
    QueryConfig config =
        QueryConfig.builder().sql("SELECT 1").timeoutSeconds(60).maxRows(1000).build();

    String result = service.toYaml(config);

    assertThat(result).contains("timeoutSeconds: 60");
    assertThat(result).contains("maxRows: 1000");
  }

  @Test
  void toYaml_withEnumParameter_includesEnumValues() {
    QueryConfig config =
        QueryConfig.builder()
            .sql("SELECT 1")
            .parameters(
                List.of(
                    ParameterConfig.builder()
                        .name("status")
                        .dataType("ENUM")
                        .enumValues(
                            List.of(
                                EnumValue.builder().value("A").description("Active").build(),
                                EnumValue.builder().value("I").description("Inactive").build()))
                        .build()))
            .build();

    String result = service.toYaml(config);

    assertThat(result).contains("enumValues:");
    assertThat(result).contains("value: A");
    assertThat(result).contains("description: Active");
  }

  @Test
  void toYaml_withValidationAndListSeparator_includesThem() {
    QueryConfig config =
        QueryConfig.builder()
            .sql("SELECT 1")
            .parameters(
                List.of(
                    ParameterConfig.builder()
                        .name("code")
                        .dataType("STRING")
                        .validation("^[A-Z]{2}$")
                        .listSeparator(",")
                        .defaultValue("US")
                        .build()))
            .build();

    String result = service.toYaml(config);

    assertThat(result).contains("validation:");
    assertThat(result).contains("listSeparator:");
    assertThat(result).contains("defaultValue: US");
  }

  @Test
  void roundTrip_parseAndToYaml_preservesData() {
    String originalYaml =
        """
        sql: SELECT * FROM test WHERE id = :id
        parameters:
          - name: id
            label: ID
            dataType: INTEGER
            required: true
            defaultValue: "1"
        timeoutSeconds: 30
        maxRows: 100
        """;

    QueryConfig parsed = service.parse(originalYaml);
    String regenerated = service.toYaml(parsed);

    assertThat(regenerated).contains("sql:");
    assertThat(regenerated).contains("name: id");
    assertThat(regenerated).contains("dataType: INTEGER");
    assertThat(regenerated).contains("timeoutSeconds: 30");
    assertThat(regenerated).contains("maxRows: 100");
  }
}
