package com.ivamare.service;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryVersion;
import com.ivamare.dto.ExportedConfig;
import com.ivamare.dto.ExportedConfig.ExportedQuery;
import com.ivamare.dto.ExportedConfig.ExportedVersion;
import com.ivamare.repository.QueryRepository;
import com.ivamare.repository.QueryVersionRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** Service for exporting query configurations to YAML. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigExportService {

  private static final String FORMAT_VERSION = "1.0";

  private final QueryRepository queryRepository;
  private final QueryVersionRepository versionRepository;

  /**
   * Export all active queries to YAML.
   *
   * @param exportedBy Username performing the export
   * @return YAML string of exported configuration
   */
  @Transactional(readOnly = true)
  public String exportAll(String exportedBy) {
    log.info("Exporting all queries by user '{}'", exportedBy);

    List<Query> queries = queryRepository.findByIsActiveTrueOrderByNameAsc();

    ExportedConfig export =
        ExportedConfig.builder()
            .formatVersion(FORMAT_VERSION)
            .exportedAt(LocalDateTime.now())
            .exportedBy(exportedBy)
            .queries(queries.stream().map(this::toExportedQuery).toList())
            .build();

    String yaml = toYaml(export);
    log.info("Exported {} queries", queries.size());
    return yaml;
  }

  /**
   * Export a single query to YAML.
   *
   * @param queryId Query ID to export
   * @param exportedBy Username performing the export
   * @return YAML string of exported query
   */
  @Transactional(readOnly = true)
  public String exportQuery(String queryId, String exportedBy) {
    log.info("Exporting query '{}' by user '{}'", queryId, exportedBy);

    Query query =
        queryRepository
            .findById(queryId)
            .orElseThrow(() -> new IllegalArgumentException("Query not found: " + queryId));

    ExportedConfig export =
        ExportedConfig.builder()
            .formatVersion(FORMAT_VERSION)
            .exportedAt(LocalDateTime.now())
            .exportedBy(exportedBy)
            .queries(List.of(toExportedQuery(query)))
            .build();

    return toYaml(export);
  }

  private ExportedQuery toExportedQuery(Query query) {
    List<QueryVersion> versions = versionRepository.findByQueryIdOrderByVersionDesc(query.getId());

    return ExportedQuery.builder()
        .id(query.getId())
        .name(query.getName())
        .description(query.getDescription())
        .category(query.getCategory())
        .connectionName(query.getConnectionName())
        .queryType(query.getQueryType().name())
        .currentVersion(query.getCurrentVersion())
        .versions(
            versions.stream()
                .map(
                    v ->
                        ExportedVersion.builder()
                            .version(v.getVersion())
                            .createdAt(v.getCreatedAt())
                            .createdBy(v.getCreatedBy())
                            .config(v.getConfigYaml())
                            .build())
                .toList())
        .build();
  }

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private String toYaml(ExportedConfig config) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    options.setIndent(2);
    options.setIndicatorIndent(1);

    Yaml yaml = new Yaml(options);
    return yaml.dump(toMap(config));
  }

  private Map<String, Object> toMap(ExportedConfig config) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("formatVersion", config.getFormatVersion());
    map.put("exportedAt", formatDateTime(config.getExportedAt()));
    map.put("exportedBy", config.getExportedBy());
    map.put("queries", config.getQueries().stream().map(this::toMap).toList());
    return map;
  }

  private Map<String, Object> toMap(ExportedQuery query) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", query.getId());
    map.put("name", query.getName());
    map.put("description", query.getDescription());
    map.put("category", query.getCategory());
    map.put("connectionName", query.getConnectionName());
    map.put("queryType", query.getQueryType());
    map.put("currentVersion", query.getCurrentVersion());
    map.put("versions", query.getVersions().stream().map(this::toMap).toList());
    return map;
  }

  private Map<String, Object> toMap(ExportedVersion version) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("version", version.getVersion());
    map.put("createdAt", formatDateTime(version.getCreatedAt()));
    map.put("createdBy", version.getCreatedBy());
    map.put("config", version.getConfig());
    return map;
  }

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
  }
}
