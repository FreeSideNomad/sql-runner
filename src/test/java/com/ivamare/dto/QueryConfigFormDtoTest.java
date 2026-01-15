package com.ivamare.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.ParameterType;
import com.ivamare.dto.QueryConfigFormDto.ParameterFormDto;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for QueryConfigFormDto. */
class QueryConfigFormDtoTest {

  @Test
  void from_withNullConfig_returnsEmptyFormDto() {
    QueryConfigFormDto result = QueryConfigFormDto.from(null);

    assertThat(result).isNotNull();
    assertThat(result.getParameters()).isEmpty();
  }

  @Test
  void from_withSelectQuery_mapsFields() {
    QueryConfig config =
        QueryConfig.builder()
            .sql("SELECT * FROM customers")
            .timeoutSeconds(60)
            .maxRows(1000)
            .build();

    QueryConfigFormDto result = QueryConfigFormDto.from(config);

    assertThat(result.getSql()).isEqualTo("SELECT * FROM customers");
    assertThat(result.getTimeoutSeconds()).isEqualTo(60);
    assertThat(result.getMaxRows()).isEqualTo(1000);
  }

  @Test
  void from_withUpdateWorkflow_mapsAllFields() {
    QueryConfig config =
        QueryConfig.builder()
            .selectSql("SELECT id, name FROM test")
            .updateSql("UPDATE test SET name = :name")
            .primaryKeyColumn("id")
            .backupColumns(List.of("name", "status"))
            .rollbackColumns(List.of("name"))
            .build();

    QueryConfigFormDto result = QueryConfigFormDto.from(config);

    assertThat(result.getSelectSql()).isEqualTo("SELECT id, name FROM test");
    assertThat(result.getUpdateSql()).isEqualTo("UPDATE test SET name = :name");
    assertThat(result.getPrimaryKeyColumn()).isEqualTo("id");
    assertThat(result.getBackupColumns()).isEqualTo("name, status");
    assertThat(result.getRollbackColumns()).isEqualTo("name");
  }

  @Test
  void from_withParameters_mapsParameters() {
    QueryConfig.ParameterConfig param =
        QueryConfig.ParameterConfig.builder()
            .name("region")
            .label("Region")
            .dataType("STRING")
            .required(true)
            .defaultValue("US")
            .validation("^[A-Z]{2}$")
            .listSeparator(",")
            .build();
    QueryConfig config = QueryConfig.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfigFormDto result = QueryConfigFormDto.from(config);

    assertThat(result.getParameters()).hasSize(1);
    ParameterFormDto p = result.getParameters().get(0);
    assertThat(p.getName()).isEqualTo("region");
    assertThat(p.getLabel()).isEqualTo("Region");
    assertThat(p.getDataType()).isEqualTo(ParameterType.STRING);
    assertThat(p.isRequired()).isTrue();
    assertThat(p.getDefaultValue()).isEqualTo("US");
    assertThat(p.getValidation()).isEqualTo("^[A-Z]{2}$");
    assertThat(p.getListSeparator()).isEqualTo(",");
  }

  @Test
  void from_withEnumParameter_formatsEnumValues() {
    QueryConfig.ParameterConfig param =
        QueryConfig.ParameterConfig.builder()
            .name("status")
            .dataType("ENUM")
            .enumValues(
                List.of(
                    QueryConfig.EnumValue.builder().value("A").description("Active").build(),
                    QueryConfig.EnumValue.builder().value("I").description("Inactive").build()))
            .build();
    QueryConfig config = QueryConfig.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfigFormDto result = QueryConfigFormDto.from(config);

    assertThat(result.getParameters()).hasSize(1);
    assertThat(result.getParameters().get(0).getEnumValues()).isEqualTo("A:Active\nI:Inactive");
  }

  @Test
  void from_withEnumParameterWithoutDescription_formatsValuesOnly() {
    QueryConfig.ParameterConfig param =
        QueryConfig.ParameterConfig.builder()
            .name("status")
            .dataType("ENUM")
            .enumValues(
                List.of(
                    QueryConfig.EnumValue.builder().value("A").build(),
                    QueryConfig.EnumValue.builder().value("I").description("").build()))
            .build();
    QueryConfig config = QueryConfig.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfigFormDto result = QueryConfigFormDto.from(config);

    assertThat(result.getParameters().get(0).getEnumValues()).isEqualTo("A\nI");
  }

  @Test
  void from_withUnknownDataType_defaultsToString() {
    QueryConfig.ParameterConfig param =
        QueryConfig.ParameterConfig.builder().name("test").dataType("UNKNOWN").build();
    QueryConfig config = QueryConfig.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfigFormDto result = QueryConfigFormDto.from(config);

    assertThat(result.getParameters().get(0).getDataType()).isEqualTo(ParameterType.STRING);
  }

  @Test
  void from_withNullDataType_defaultsToString() {
    QueryConfig.ParameterConfig param =
        QueryConfig.ParameterConfig.builder().name("test").dataType(null).build();
    QueryConfig config = QueryConfig.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfigFormDto result = QueryConfigFormDto.from(config);

    assertThat(result.getParameters().get(0).getDataType()).isEqualTo(ParameterType.STRING);
  }

  @Test
  void from_withEmptyDataType_defaultsToString() {
    QueryConfig.ParameterConfig param =
        QueryConfig.ParameterConfig.builder().name("test").dataType("").build();
    QueryConfig config = QueryConfig.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfigFormDto result = QueryConfigFormDto.from(config);

    assertThat(result.getParameters().get(0).getDataType()).isEqualTo(ParameterType.STRING);
  }

  @Test
  void toQueryConfig_withSelectQuery_convertsFields() {
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .sql("SELECT * FROM test")
            .timeoutSeconds(30)
            .maxRows(500)
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getSql()).isEqualTo("SELECT * FROM test");
    assertThat(result.getTimeoutSeconds()).isEqualTo(30);
    assertThat(result.getMaxRows()).isEqualTo(500);
  }

  @Test
  void toQueryConfig_withUpdateWorkflow_convertsAllFields() {
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .selectSql("SELECT id FROM test")
            .updateSql("UPDATE test SET name = :name")
            .primaryKeyColumn("id")
            .backupColumns("name, status")
            .rollbackColumns("name")
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getSelectSql()).isEqualTo("SELECT id FROM test");
    assertThat(result.getUpdateSql()).isEqualTo("UPDATE test SET name = :name");
    assertThat(result.getPrimaryKeyColumn()).isEqualTo("id");
    assertThat(result.getBackupColumns()).containsExactly("name", "status");
    assertThat(result.getRollbackColumns()).containsExactly("name");
  }

  @Test
  void toQueryConfig_withParameters_convertsParameters() {
    ParameterFormDto param =
        ParameterFormDto.builder()
            .name("region")
            .label("Region")
            .dataType(ParameterType.STRING)
            .required(true)
            .defaultValue("US")
            .validation("^[A-Z]{2}$")
            .listSeparator(",")
            .build();
    QueryConfigFormDto form =
        QueryConfigFormDto.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getParameters()).hasSize(1);
    QueryConfig.ParameterConfig p = result.getParameters().get(0);
    assertThat(p.getName()).isEqualTo("region");
    assertThat(p.getLabel()).isEqualTo("Region");
    assertThat(p.getDataType()).isEqualTo("STRING");
    assertThat(p.isRequired()).isTrue();
    assertThat(p.getDefaultValue()).isEqualTo("US");
  }

  @Test
  void toQueryConfig_withEnumValues_parsesEnumValues() {
    ParameterFormDto param =
        ParameterFormDto.builder()
            .name("status")
            .dataType(ParameterType.ENUM)
            .enumValues("A:Active\nI:Inactive")
            .build();
    QueryConfigFormDto form =
        QueryConfigFormDto.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfig result = form.toQueryConfig();

    QueryConfig.ParameterConfig p = result.getParameters().get(0);
    assertThat(p.getEnumValues()).hasSize(2);
    assertThat(p.getEnumValues().get(0).getValue()).isEqualTo("A");
    assertThat(p.getEnumValues().get(0).getDescription()).isEqualTo("Active");
    assertThat(p.getEnumValues().get(1).getValue()).isEqualTo("I");
    assertThat(p.getEnumValues().get(1).getDescription()).isEqualTo("Inactive");
  }

  @Test
  void toQueryConfig_withEnumValuesWithoutDescription_parsesValuesOnly() {
    ParameterFormDto param =
        ParameterFormDto.builder()
            .name("status")
            .dataType(ParameterType.ENUM)
            .enumValues("A\nI\nP")
            .build();
    QueryConfigFormDto form =
        QueryConfigFormDto.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfig result = form.toQueryConfig();

    QueryConfig.ParameterConfig p = result.getParameters().get(0);
    assertThat(p.getEnumValues()).hasSize(3);
    assertThat(p.getEnumValues().get(0).getValue()).isEqualTo("A");
    assertThat(p.getEnumValues().get(0).getDescription()).isNull();
  }

  @Test
  void toQueryConfig_skipsEmptyParameterNames() {
    ParameterFormDto param1 =
        ParameterFormDto.builder().name("valid").dataType(ParameterType.STRING).build();
    ParameterFormDto param2 =
        ParameterFormDto.builder().name("").dataType(ParameterType.STRING).build();
    ParameterFormDto param3 =
        ParameterFormDto.builder().name("   ").dataType(ParameterType.STRING).build();
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .sql("SELECT 1")
            .parameters(List.of(param1, param2, param3))
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getParameters()).hasSize(1);
    assertThat(result.getParameters().get(0).getName()).isEqualTo("valid");
  }

  @Test
  void toQueryConfig_withNullDataType_defaultsToString() {
    ParameterFormDto param = ParameterFormDto.builder().name("test").dataType(null).build();
    QueryConfigFormDto form =
        QueryConfigFormDto.builder().sql("SELECT 1").parameters(List.of(param)).build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getParameters().get(0).getDataType()).isEqualTo("STRING");
  }

  @Test
  void toQueryConfig_withEmptyColumnLists_returnsNull() {
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .sql("SELECT 1")
            .backupColumns("   ")
            .rollbackColumns("")
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getBackupColumns()).isNull();
    assertThat(result.getRollbackColumns()).isNull();
  }

  @Test
  void toQueryConfig_withNullParameters_handlesGracefully() {
    QueryConfigFormDto form = QueryConfigFormDto.builder().sql("SELECT 1").parameters(null).build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getParameters()).isNull();
  }

  @Test
  void roundTrip_fromAndToQueryConfig_preservesData() {
    QueryConfig original =
        QueryConfig.builder()
            .sql("SELECT * FROM test WHERE region = :region")
            .parameters(
                List.of(
                    QueryConfig.ParameterConfig.builder()
                        .name("region")
                        .label("Region")
                        .dataType("STRING")
                        .required(true)
                        .defaultValue("US")
                        .build()))
            .timeoutSeconds(60)
            .maxRows(1000)
            .build();

    QueryConfigFormDto form = QueryConfigFormDto.from(original);
    QueryConfig result = form.toQueryConfig();

    assertThat(result.getSql()).isEqualTo(original.getSql());
    assertThat(result.getTimeoutSeconds()).isEqualTo(original.getTimeoutSeconds());
    assertThat(result.getMaxRows()).isEqualTo(original.getMaxRows());
    assertThat(result.getParameters()).hasSize(1);
    assertThat(result.getParameters().get(0).getName()).isEqualTo("region");
    assertThat(result.getParameters().get(0).isRequired()).isTrue();
  }

  @Test
  void toQueryConfig_withUpdateBindingModeBatch_parsesMode() {
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .selectSql("SELECT id FROM test")
            .updateSql("UPDATE test SET x = 1 WHERE id IN (:id_list)")
            .updateBindingMode("BATCH")
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getUpdateBindingMode()).isEqualTo(com.ivamare.domain.UpdateBindingMode.BATCH);
  }

  @Test
  void toQueryConfig_withUpdateBindingModeRowByRow_parsesMode() {
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .selectSql("SELECT id, name FROM test")
            .updateSql("UPDATE test SET name = :name WHERE id = :id")
            .updateBindingMode("ROW_BY_ROW")
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getUpdateBindingMode())
        .isEqualTo(com.ivamare.domain.UpdateBindingMode.ROW_BY_ROW);
  }

  @Test
  void toQueryConfig_withInvalidUpdateBindingMode_returnsNull() {
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .selectSql("SELECT * FROM test")
            .updateSql("UPDATE test SET x = 1")
            .updateBindingMode("INVALID_MODE")
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getUpdateBindingMode()).isNull();
  }

  @Test
  void toQueryConfig_withNullUpdateBindingMode_returnsNull() {
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .selectSql("SELECT * FROM test")
            .updateSql("UPDATE test SET x = 1")
            .updateBindingMode(null)
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getUpdateBindingMode()).isNull();
  }

  @Test
  void toQueryConfig_withBlankUpdateBindingMode_returnsNull() {
    QueryConfigFormDto form =
        QueryConfigFormDto.builder()
            .selectSql("SELECT * FROM test")
            .updateSql("UPDATE test SET x = 1")
            .updateBindingMode("   ")
            .build();

    QueryConfig result = form.toQueryConfig();

    assertThat(result.getUpdateBindingMode()).isNull();
  }

  @Test
  void from_withUpdateBindingMode_preservesMode() {
    QueryConfig original =
        QueryConfig.builder()
            .selectSql("SELECT id FROM test")
            .updateSql("UPDATE test SET x = 1")
            .updateBindingMode(com.ivamare.domain.UpdateBindingMode.BATCH)
            .build();

    QueryConfigFormDto form = QueryConfigFormDto.from(original);

    assertThat(form.getUpdateBindingMode()).isEqualTo("BATCH");
  }

  @Test
  void from_withNullUpdateBindingMode_preservesNull() {
    QueryConfig original =
        QueryConfig.builder().selectSql("SELECT id FROM test").updateBindingMode(null).build();

    QueryConfigFormDto form = QueryConfigFormDto.from(original);

    assertThat(form.getUpdateBindingMode()).isNull();
  }
}
