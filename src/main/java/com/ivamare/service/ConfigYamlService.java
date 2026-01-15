package com.ivamare.service;

import com.ivamare.dto.QueryConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** Service for converting between QueryConfig and YAML. */
@Service
@Slf4j
public class ConfigYamlService {

  /** Parse YAML string into QueryConfig. */
  public QueryConfig parse(String yaml) {
    if (yaml == null || yaml.isBlank()) {
      return new QueryConfig();
    }
    try {
      Yaml parser = new Yaml();
      return parser.loadAs(yaml, QueryConfig.class);
    } catch (Exception e) {
      log.error("Failed to parse config YAML: {}", e.getMessage());
      throw new IllegalArgumentException("Invalid config YAML: " + e.getMessage(), e);
    }
  }

  /** Convert QueryConfig to YAML string. */
  public String toYaml(QueryConfig config) {
    if (config == null) {
      return "";
    }

    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    options.setIndent(2);
    options.setIndicatorIndent(1);
    options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

    Yaml yaml = new Yaml(options);

    // Build ordered map for consistent output
    Map<String, Object> configMap = new LinkedHashMap<>();

    // SQL fields
    if (config.getSql() != null && !config.getSql().isBlank()) {
      configMap.put("sql", config.getSql());
    }
    if (config.getSelectSql() != null && !config.getSelectSql().isBlank()) {
      configMap.put("selectSql", config.getSelectSql());
    }
    if (config.getUpdateSql() != null && !config.getUpdateSql().isBlank()) {
      configMap.put("updateSql", config.getUpdateSql());
    }
    if (config.getUpdateBindingMode() != null) {
      configMap.put("updateBindingMode", config.getUpdateBindingMode().name());
    }

    // Parameters
    if (config.getParameters() != null && !config.getParameters().isEmpty()) {
      List<Map<String, Object>> params =
          config.getParameters().stream().map(this::paramToMap).collect(Collectors.toList());
      configMap.put("parameters", params);
    }

    // UPDATE_WORKFLOW specific fields
    if (config.getPrimaryKeyColumn() != null && !config.getPrimaryKeyColumn().isBlank()) {
      configMap.put("primaryKeyColumn", config.getPrimaryKeyColumn());
    }
    if (config.getBackupColumns() != null && !config.getBackupColumns().isEmpty()) {
      configMap.put("backupColumns", config.getBackupColumns());
    }
    if (config.getRollbackColumns() != null && !config.getRollbackColumns().isEmpty()) {
      configMap.put("rollbackColumns", config.getRollbackColumns());
    }

    // Optional settings
    if (config.getTimeoutSeconds() != null) {
      configMap.put("timeoutSeconds", config.getTimeoutSeconds());
    }
    if (config.getMaxRows() != null) {
      configMap.put("maxRows", config.getMaxRows());
    }

    return yaml.dump(configMap);
  }

  private Map<String, Object> paramToMap(QueryConfig.ParameterConfig param) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", param.getName());

    if (param.getLabel() != null && !param.getLabel().isBlank()) {
      map.put("label", param.getLabel());
    }
    if (param.getDataType() != null && !param.getDataType().isBlank()) {
      map.put("dataType", param.getDataType());
    }
    map.put("required", param.isRequired());

    if (param.getDefaultValue() != null && !param.getDefaultValue().isBlank()) {
      map.put("defaultValue", param.getDefaultValue());
    }
    if (param.getValidation() != null && !param.getValidation().isBlank()) {
      map.put("validation", param.getValidation());
    }
    if (param.getListSeparator() != null && !param.getListSeparator().isBlank()) {
      map.put("listSeparator", param.getListSeparator());
    }
    if (param.getEnumValues() != null && !param.getEnumValues().isEmpty()) {
      List<Map<String, String>> enumVals =
          param.getEnumValues().stream()
              .map(
                  ev -> {
                    Map<String, String> evMap = new LinkedHashMap<>();
                    evMap.put("value", ev.getValue());
                    if (ev.getDescription() != null && !ev.getDescription().isBlank()) {
                      evMap.put("description", ev.getDescription());
                    }
                    return evMap;
                  })
              .collect(Collectors.toList());
      map.put("enumValues", enumVals);
    }

    return map;
  }
}
