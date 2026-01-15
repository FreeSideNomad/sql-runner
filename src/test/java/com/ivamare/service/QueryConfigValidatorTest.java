package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.QueryType;
import com.ivamare.dto.QueryConfigFormDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QueryConfigValidatorTest {

  private QueryConfigValidator validator;
  private UpdateParameterAnalyzer analyzer;

  @BeforeEach
  void setUp() {
    analyzer = new UpdateParameterAnalyzer();
    validator = new QueryConfigValidator(analyzer);
  }

  @Nested
  class ValidateUpdateConfig {

    @Test
    void returnsEmptyResult_forNonUpdateWorkflow() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("BATCH");

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.SELECT);

      assertThat(result.hasErrors()).isFalse();
      assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    void returnsError_whenUpdateBindingModeIsNull() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode(null);

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isTrue();
      assertThat(result.getErrors())
          .containsExactly("Update binding mode is required for UPDATE_WORKFLOW queries");
    }

    @Test
    void returnsError_whenUpdateBindingModeIsInvalid() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("INVALID_MODE");

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isTrue();
      assertThat(result.getErrors()).containsExactly("Invalid update binding mode: INVALID_MODE");
    }
  }

  @Nested
  class BatchModeValidation {

    @Test
    void returnsError_whenIdListMissing() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("BATCH");
      config.setUpdateSql("UPDATE test SET status = :status WHERE id = :id");
      config.setPrimaryKeyColumn("id");

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isTrue();
      assertThat(result.getErrors())
          .containsExactly("Batch mode requires :id_list parameter in UPDATE SQL");
    }

    @Test
    void returnsError_whenPrimaryKeyColumnMissing() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("BATCH");
      config.setUpdateSql("UPDATE test SET status = :status WHERE id IN (:id_list)");
      config.setPrimaryKeyColumn(null);

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isTrue();
      assertThat(result.getErrors())
          .containsExactly("Batch mode requires Primary Key Column to be defined");
    }

    @Test
    void returnsMultipleErrors_whenBothIdListAndPrimaryKeyMissing() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("BATCH");
      config.setUpdateSql("UPDATE test SET status = :status WHERE id = :id");
      config.setPrimaryKeyColumn("");

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isTrue();
      assertThat(result.getErrors()).hasSize(2);
      assertThat(result.getErrors())
          .contains(
              "Batch mode requires :id_list parameter in UPDATE SQL",
              "Batch mode requires Primary Key Column to be defined");
    }

    @Test
    void passesValidation_whenBatchModeConfiguredCorrectly() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("BATCH");
      config.setUpdateSql("UPDATE test SET status = :status WHERE id IN (:id_list)");
      config.setPrimaryKeyColumn("id");

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isFalse();
    }
  }

  @Nested
  class RowByRowModeValidation {

    @Test
    void returnsError_whenNoColumnParamsFound() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("ROW_BY_ROW");
      config.setSelectSql("SELECT id, name FROM test");
      config.setUpdateSql("UPDATE test SET status = :newStatus WHERE region = :region");

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isTrue();
      assertThat(result.getErrors())
          .containsExactly(
              "Row-by-row mode requires at least one column parameter (e.g., :id, :name) in UPDATE SQL that matches a SELECT column");
    }

    @Test
    void passesValidation_whenColumnParamsFound() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("ROW_BY_ROW");
      config.setSelectSql("SELECT id, name FROM test");
      config.setUpdateSql("UPDATE test SET name = UPPER(:name) WHERE id = :id");
      config.setPrimaryKeyColumn("id");

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isFalse();
      assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    void returnsWarning_whenPrimaryKeyNotInWhereClause() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("ROW_BY_ROW");
      config.setSelectSql("SELECT id, name FROM test");
      config.setUpdateSql("UPDATE test SET name = UPPER(:name)");
      config.setPrimaryKeyColumn("id");

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isFalse();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings())
          .containsExactly("Primary key column :id is not used in UPDATE SQL WHERE clause");
    }
  }

  @Nested
  class StandardModeValidation {

    @Test
    void passesValidation_whenAllParamsDefinedInConfig() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("STANDARD");
      config.setUpdateSql("UPDATE test SET status = :newStatus WHERE region = :region");

      QueryConfigFormDto.ParameterFormDto param1 = new QueryConfigFormDto.ParameterFormDto();
      param1.setName("newStatus");
      QueryConfigFormDto.ParameterFormDto param2 = new QueryConfigFormDto.ParameterFormDto();
      param2.setName("region");
      config.setParameters(List.of(param1, param2));

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isFalse();
      assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    void returnsWarning_whenParamsNotDefined() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("STANDARD");
      config.setUpdateSql("UPDATE test SET status = :newStatus WHERE region = :region");
      config.setParameters(List.of());

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isFalse();
      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings())
          .contains(
              "Parameter :newstatus in UPDATE SQL is not defined in parameters configuration",
              "Parameter :region in UPDATE SQL is not defined in parameters configuration");
    }

    @Test
    void handlesNullParameters() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("STANDARD");
      config.setUpdateSql("UPDATE test SET status = :status");
      config.setParameters(null);

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isFalse();
      assertThat(result.hasWarnings()).isTrue();
    }

    @Test
    void ignoresBlankParameterNames() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setUpdateBindingMode("STANDARD");
      config.setUpdateSql("UPDATE test SET status = :status");

      QueryConfigFormDto.ParameterFormDto param1 = new QueryConfigFormDto.ParameterFormDto();
      param1.setName("");
      QueryConfigFormDto.ParameterFormDto param2 = new QueryConfigFormDto.ParameterFormDto();
      param2.setName(null);
      QueryConfigFormDto.ParameterFormDto param3 = new QueryConfigFormDto.ParameterFormDto();
      param3.setName("status");
      config.setParameters(List.of(param1, param2, param3));

      QueryConfigValidator.ValidationResult result =
          validator.validateUpdateConfig(config, QueryType.UPDATE_WORKFLOW);

      assertThat(result.hasErrors()).isFalse();
      assertThat(result.hasWarnings()).isFalse();
    }
  }

  @Nested
  class GetAvailableParameters {

    @Test
    void returnsUserParameters() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      QueryConfigFormDto.ParameterFormDto param1 = new QueryConfigFormDto.ParameterFormDto();
      param1.setName("region");
      QueryConfigFormDto.ParameterFormDto param2 = new QueryConfigFormDto.ParameterFormDto();
      param2.setName("status");
      config.setParameters(List.of(param1, param2));
      config.setSelectSql("SELECT id, name FROM test");

      QueryConfigValidator.AvailableParameters params = validator.getAvailableParameters(config);

      assertThat(params.getUserParameters()).containsExactlyInAnyOrder("region", "status");
    }

    @Test
    void returnsColumnParameters() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setParameters(List.of());
      config.setSelectSql("SELECT id, name, email FROM test");

      QueryConfigValidator.AvailableParameters params = validator.getAvailableParameters(config);

      assertThat(params.getColumnParameters()).containsExactlyInAnyOrder("id", "name", "email");
    }

    @Test
    void handlesNullParameters() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      config.setParameters(null);
      config.setSelectSql("SELECT id FROM test");

      QueryConfigValidator.AvailableParameters params = validator.getAvailableParameters(config);

      assertThat(params.getUserParameters()).isEmpty();
      assertThat(params.getColumnParameters()).containsExactly("id");
    }

    @Test
    void ignoresBlankParameterNames() {
      QueryConfigFormDto config = new QueryConfigFormDto();
      QueryConfigFormDto.ParameterFormDto param1 = new QueryConfigFormDto.ParameterFormDto();
      param1.setName("");
      QueryConfigFormDto.ParameterFormDto param2 = new QueryConfigFormDto.ParameterFormDto();
      param2.setName(null);
      QueryConfigFormDto.ParameterFormDto param3 = new QueryConfigFormDto.ParameterFormDto();
      param3.setName("validParam");
      config.setParameters(List.of(param1, param2, param3));
      config.setSelectSql("SELECT id FROM test");

      QueryConfigValidator.AvailableParameters params = validator.getAvailableParameters(config);

      assertThat(params.getUserParameters()).containsExactly("validParam");
    }
  }

  @Nested
  class AvailableParametersDisplay {

    @Test
    void userParametersDisplay_showsNone_whenEmpty() {
      QueryConfigValidator.AvailableParameters params =
          new QueryConfigValidator.AvailableParameters(java.util.Set.of(), java.util.Set.of("id"));

      assertThat(params.getUserParametersDisplay()).isEqualTo("None");
    }

    @Test
    void userParametersDisplay_showsFormattedParams() {
      QueryConfigValidator.AvailableParameters params =
          new QueryConfigValidator.AvailableParameters(
              java.util.Set.of("region", "status"), java.util.Set.of("id"));

      String display = params.getUserParametersDisplay();
      assertThat(display).contains(":region");
      assertThat(display).contains(":status");
    }

    @Test
    void columnParametersDisplay_showsMessage_whenEmpty() {
      QueryConfigValidator.AvailableParameters params =
          new QueryConfigValidator.AvailableParameters(
              java.util.Set.of("region"), java.util.Set.of());

      assertThat(params.getColumnParametersDisplay())
          .isEqualTo("None (SELECT columns will be detected at runtime)");
    }

    @Test
    void columnParametersDisplay_showsFormattedParams() {
      QueryConfigValidator.AvailableParameters params =
          new QueryConfigValidator.AvailableParameters(
              java.util.Set.of(), java.util.Set.of("id", "name"));

      String display = params.getColumnParametersDisplay();
      assertThat(display).contains(":id");
      assertThat(display).contains(":name");
    }
  }

  @Nested
  class ValidationResult {

    @Test
    void hasErrors_returnsFalse_whenEmpty() {
      QueryConfigValidator.ValidationResult result = new QueryConfigValidator.ValidationResult();

      assertThat(result.hasErrors()).isFalse();
      assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void hasErrors_returnsTrue_whenErrorsExist() {
      QueryConfigValidator.ValidationResult result = new QueryConfigValidator.ValidationResult();
      result.addError("Test error");

      assertThat(result.hasErrors()).isTrue();
      assertThat(result.getErrors()).containsExactly("Test error");
    }

    @Test
    void hasWarnings_returnsFalse_whenEmpty() {
      QueryConfigValidator.ValidationResult result = new QueryConfigValidator.ValidationResult();

      assertThat(result.hasWarnings()).isFalse();
      assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void hasWarnings_returnsTrue_whenWarningsExist() {
      QueryConfigValidator.ValidationResult result = new QueryConfigValidator.ValidationResult();
      result.addWarning("Test warning");

      assertThat(result.hasWarnings()).isTrue();
      assertThat(result.getWarnings()).containsExactly("Test warning");
    }
  }
}
