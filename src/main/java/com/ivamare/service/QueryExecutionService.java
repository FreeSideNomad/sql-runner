package com.ivamare.service;

import com.ivamare.config.ConnectionProperties.ConnectionConfig;
import com.ivamare.domain.*;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.dto.QueryConfig;
import com.ivamare.dto.QueryConfig.ParameterConfig;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/** Service for executing queries against configured databases. */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryExecutionService {

  private final ConnectionRegistry connectionRegistry;
  private final ExecutionLogService logService;
  private final QueryService queryService;

  private final Map<String, Statement> activeStatements = new ConcurrentHashMap<>();
  private final Map<String, Future<?>> activeExecutions = new ConcurrentHashMap<>();

  /**
   * Execute a SELECT query.
   *
   * @param queryId Query ID
   * @param rawParams Raw parameter values from form
   * @param executedBy Username executing the query
   * @return ExecutionResult with rows or error
   */
  public ExecutionResult executeSelect(
      String queryId, Map<String, String> rawParams, String executedBy) {

    Query query = queryService.getQuery(queryId);
    String configYaml = queryService.getCurrentConfigYaml(queryId);
    QueryConfig config = parseConfig(configYaml);

    log.info("Executing SELECT query '{}' by user '{}'", query.getName(), executedBy);

    // Convert parameters
    Map<String, Object> params = convertParameters(rawParams, config.getParameters());

    // Get DataSource and create template
    DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());
    NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(ds);
    ConnectionConfig connConfig = connectionRegistry.getConnectionConfig(query.getConnectionName());

    String executionId = UUID.randomUUID().toString();
    long startTime = System.currentTimeMillis();

    try {
      // Execute query
      String sql =
          adaptSqlForDatabase(
              config.getExecutableSql(), connConfig != null ? connConfig.getType() : null);
      List<Map<String, Object>> results = jdbc.queryForList(sql, params);
      long duration = System.currentTimeMillis() - startTime;

      // Log successful execution
      ExecutionLog logEntry =
          logService.logSelectSuccess(query, params, results.size(), duration, executedBy);

      log.info(
          "Query '{}' executed successfully: {} rows in {}ms",
          query.getName(),
          results.size(),
          duration);

      return ExecutionResult.success(results, duration, logEntry.getId());

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;

      // Log failed execution
      ExecutionLog logEntry =
          logService.logSelectFailure(query, params, duration, e.getMessage(), executedBy);

      log.error("Query '{}' failed: {}", query.getName(), e.getMessage());

      return ExecutionResult.failure(e.getMessage(), duration, logEntry.getId());
    }
  }

  /**
   * Execute a SELECT query with timeout.
   *
   * @param queryId Query ID
   * @param rawParams Raw parameter values
   * @param executedBy Username
   * @param timeoutSeconds Timeout in seconds
   * @return ExecutionResult
   */
  public ExecutionResult executeSelectWithTimeout(
      String queryId, Map<String, String> rawParams, String executedBy, int timeoutSeconds) {

    String executionId = UUID.randomUUID().toString();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<ExecutionResult> future =
        executor.submit(() -> executeSelect(queryId, rawParams, executedBy));

    activeExecutions.put(executionId, future);

    try {
      return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      log.warn("Query '{}' timed out after {}s", queryId, timeoutSeconds);
      return ExecutionResult.failure(
          "Query timed out after " + timeoutSeconds + " seconds", timeoutSeconds * 1000L, null);
    } catch (Exception e) {
      log.error("Query execution error", e);
      return ExecutionResult.failure(e.getMessage(), 0, null);
    } finally {
      activeExecutions.remove(executionId);
      executor.shutdownNow();
    }
  }

  /**
   * Cancel a running query execution.
   *
   * @param executionId The execution ID to cancel
   * @return true if cancelled, false if not found
   */
  public boolean cancelExecution(String executionId) {
    Future<?> future = activeExecutions.remove(executionId);
    if (future != null) {
      log.info("Cancelling execution '{}'", executionId);
      return future.cancel(true);
    }
    return false;
  }

  /** Get the number of currently active executions. */
  public int getActiveExecutionCount() {
    return activeExecutions.size();
  }

  /**
   * Parse YAML configuration into QueryConfig.
   *
   * @param configYaml YAML string
   * @return Parsed configuration
   */
  public QueryConfig parseConfig(String configYaml) {
    Yaml yaml = new Yaml();
    Map<String, Object> map = yaml.load(configYaml);

    QueryConfig.QueryConfigBuilder builder = QueryConfig.builder();

    if (map.containsKey("sql")) {
      builder.sql((String) map.get("sql"));
    }
    if (map.containsKey("selectSql")) {
      builder.selectSql((String) map.get("selectSql"));
    }
    if (map.containsKey("updateSql")) {
      builder.updateSql((String) map.get("updateSql"));
    }
    if (map.containsKey("primaryKeyColumn")) {
      builder.primaryKeyColumn((String) map.get("primaryKeyColumn"));
    }
    if (map.containsKey("timeoutSeconds")) {
      builder.timeoutSeconds((Integer) map.get("timeoutSeconds"));
    }
    if (map.containsKey("maxRows")) {
      builder.maxRows((Integer) map.get("maxRows"));
    }

    // Parse parameters
    if (map.containsKey("parameters")) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> paramsList = (List<Map<String, Object>>) map.get("parameters");
      List<ParameterConfig> parameters = new ArrayList<>();

      for (Map<String, Object> paramMap : paramsList) {
        ParameterConfig.ParameterConfigBuilder paramBuilder = ParameterConfig.builder();
        paramBuilder.name((String) paramMap.get("name"));
        paramBuilder.label((String) paramMap.get("label"));
        paramBuilder.dataType((String) paramMap.get("dataType"));
        paramBuilder.required(Boolean.TRUE.equals(paramMap.get("required")));
        paramBuilder.defaultValue((String) paramMap.get("defaultValue"));
        paramBuilder.validation((String) paramMap.get("validation"));
        paramBuilder.listSeparator((String) paramMap.get("listSeparator"));

        // Parse enum values
        if (paramMap.containsKey("enumValues")) {
          @SuppressWarnings("unchecked")
          List<Map<String, String>> enumList =
              (List<Map<String, String>>) paramMap.get("enumValues");
          List<QueryConfig.EnumValue> enumValues = new ArrayList<>();
          for (Map<String, String> enumMap : enumList) {
            enumValues.add(
                QueryConfig.EnumValue.builder()
                    .value(enumMap.get("value"))
                    .description(enumMap.get("description"))
                    .build());
          }
          paramBuilder.enumValues(enumValues);
        }

        parameters.add(paramBuilder.build());
      }
      builder.parameters(parameters);
    } else {
      builder.parameters(List.of());
    }

    // Parse update binding mode
    if (map.containsKey("updateBindingMode")) {
      String modeStr = (String) map.get("updateBindingMode");
      try {
        builder.updateBindingMode(
            com.ivamare.domain.UpdateBindingMode.valueOf(modeStr.toUpperCase()));
      } catch (IllegalArgumentException e) {
        // Invalid mode, leave as null (will default to STANDARD)
      }
    }

    // Parse backup/rollback columns
    if (map.containsKey("backupColumns")) {
      @SuppressWarnings("unchecked")
      List<String> cols = (List<String>) map.get("backupColumns");
      builder.backupColumns(cols);
    }
    if (map.containsKey("rollbackColumns")) {
      @SuppressWarnings("unchecked")
      List<String> cols = (List<String>) map.get("rollbackColumns");
      builder.rollbackColumns(cols);
    }

    return builder.build();
  }

  String adaptSqlForDatabase(String sql, DatabaseType dbType) {
    if (sql == null || dbType == null) {
      return sql;
    }
    if (dbType == DatabaseType.SQLSERVER) {
      return rewriteSqlServerTopParameter(sql);
    }
    return sql;
  }

  private String rewriteSqlServerTopParameter(String sql) {
    Pattern topParamPattern = Pattern.compile("\\bTOP\\s*:(\\w+)", Pattern.CASE_INSENSITIVE);
    return topParamPattern.matcher(sql).replaceAll("TOP (:$1)");
  }

  /**
   * Convert raw string parameters to typed objects.
   *
   * @param rawParams Raw string values
   * @param configs Parameter configurations
   * @return Converted parameters
   */
  public Map<String, Object> convertParameters(
      Map<String, String> rawParams, List<ParameterConfig> configs) {

    Map<String, Object> converted = new HashMap<>();

    if (configs == null) {
      return converted;
    }

    for (ParameterConfig pc : configs) {
      String rawValue = rawParams.get(pc.getName());

      if (rawValue == null || rawValue.isEmpty()) {
        if (pc.getDefaultValue() != null && !pc.getDefaultValue().isEmpty()) {
          rawValue = pc.getDefaultValue();
        } else if (pc.isRequired()) {
          throw new IllegalArgumentException("Required parameter missing: " + pc.getName());
        } else {
          converted.put(pc.getName(), null);
          continue;
        }
      }

      Object value = convertValue(rawValue, pc.getDataType(), pc.getListSeparator());
      converted.put(pc.getName(), value);
    }

    return converted;
  }

  private Object convertValue(String rawValue, String dataType, String listSeparator) {
    if (rawValue == null || dataType == null) {
      return rawValue;
    }

    return switch (dataType.toUpperCase()) {
      case "STRING", "ENUM" -> rawValue;
      case "INTEGER" -> Integer.parseInt(rawValue.trim());
      case "DECIMAL" -> new BigDecimal(rawValue.trim());
      case "DATE" -> LocalDate.parse(rawValue.trim());
      case "DATETIME" -> LocalDateTime.parse(rawValue.trim());
      case "BOOLEAN" -> Boolean.parseBoolean(rawValue.trim());
      case "LIST_STRING" -> parseList(rawValue, listSeparator);
      case "LIST_INTEGER" ->
          parseList(rawValue, listSeparator).stream().map(s -> Integer.parseInt(s.trim())).toList();
      default -> rawValue;
    };
  }

  private List<String> parseList(String rawValue, String listSeparator) {
    String separator = listSeparator != null ? listSeparator : "BOTH";

    String regex =
        switch (separator.toUpperCase()) {
          case "NEWLINE" -> "\n";
          case "COMMA" -> ",";
          default -> "[,\n]"; // BOTH
        };

    return Arrays.stream(rawValue.split(regex))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }
}
