package com.ivamare.service;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.domain.QueryVersion;
import com.ivamare.dto.ExportedConfig;
import com.ivamare.dto.ExportedConfig.ExportedQuery;
import com.ivamare.dto.ExportedConfig.ExportedVersion;
import com.ivamare.repository.QueryRepository;
import com.ivamare.repository.QueryVersionRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

/** Service for importing query configurations from YAML. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigImportService {

  private final QueryRepository queryRepository;
  private final QueryVersionRepository versionRepository;

  /**
   * Validate import YAML and return any issues.
   *
   * @param yamlContent YAML content to validate
   * @return Import result with validation info
   */
  public ImportValidationResult validateImport(String yamlContent) {
    try {
      ExportedConfig config = parseYaml(yamlContent);
      return validateConfig(config);
    } catch (Exception e) {
      log.error("Failed to parse import YAML: {}", e.getMessage());
      return ImportValidationResult.error("Failed to parse YAML: " + e.getMessage());
    }
  }

  /**
   * Import queries from YAML.
   *
   * @param yamlContent YAML content
   * @param importedBy Username performing the import
   * @return Import result
   */
  @Transactional
  public ImportResult importQueries(String yamlContent, String importedBy) {
    log.info("Importing queries by user '{}'", importedBy);

    ExportedConfig config;
    try {
      config = parseYaml(yamlContent);
    } catch (Exception e) {
      return ImportResult.failure("Failed to parse YAML: " + e.getMessage());
    }

    ImportValidationResult validation = validateConfig(config);
    if (!validation.isValid()) {
      return ImportResult.failure(
          "Validation failed: " + String.join(", ", validation.getErrors()));
    }

    int created = 0;
    int updated = 0;
    int skipped = 0;
    List<String> messages = new ArrayList<>();

    for (ExportedQuery eq : config.getQueries()) {
      Optional<Query> existing = queryRepository.findById(eq.getId());

      if (existing.isEmpty()) {
        // Create new query
        createQuery(eq, importedBy);
        created++;
        messages.add("Created: " + eq.getName());
      } else {
        Query query = existing.get();
        if (eq.getCurrentVersion() > query.getCurrentVersion()) {
          // Import newer versions
          updateQuery(query, eq, importedBy);
          updated++;
          messages.add("Updated: " + eq.getName() + " to v" + eq.getCurrentVersion());
        } else {
          skipped++;
          messages.add("Skipped: " + eq.getName() + " (version not newer)");
        }
      }
    }

    log.info("Import complete: {} created, {} updated, {} skipped", created, updated, skipped);

    return ImportResult.success(created, updated, skipped, messages);
  }

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private ExportedConfig parseYaml(String yamlContent) {
    // Strip class tags if present (from old exports)
    String cleanYaml = yamlContent.replaceAll("!!\\S+\\s*\n?", "");

    Yaml yaml = new Yaml();
    Map<String, Object> map = yaml.load(cleanYaml);

    return mapToConfig(map);
  }

  @SuppressWarnings("unchecked")
  private ExportedConfig mapToConfig(Map<String, Object> map) {
    if (map == null) {
      return null;
    }

    List<ExportedQuery> queries = new ArrayList<>();
    Object queriesObj = map.get("queries");
    if (queriesObj instanceof List<?> queryList) {
      for (Object q : queryList) {
        if (q instanceof Map<?, ?> queryMap) {
          queries.add(mapToQuery((Map<String, Object>) queryMap));
        }
      }
    }

    return ExportedConfig.builder()
        .formatVersion((String) map.get("formatVersion"))
        .exportedAt(parseDateTime(map.get("exportedAt")))
        .exportedBy((String) map.get("exportedBy"))
        .queries(queries)
        .build();
  }

  @SuppressWarnings("unchecked")
  private ExportedQuery mapToQuery(Map<String, Object> map) {
    List<ExportedVersion> versions = new ArrayList<>();
    Object versionsObj = map.get("versions");
    if (versionsObj instanceof List<?> versionList) {
      for (Object v : versionList) {
        if (v instanceof Map<?, ?> versionMap) {
          versions.add(mapToVersion((Map<String, Object>) versionMap));
        }
      }
    }

    return ExportedQuery.builder()
        .id((String) map.get("id"))
        .name((String) map.get("name"))
        .description((String) map.get("description"))
        .category((String) map.get("category"))
        .connectionName((String) map.get("connectionName"))
        .queryType((String) map.get("queryType"))
        .currentVersion(toInteger(map.get("currentVersion")))
        .versions(versions)
        .build();
  }

  private ExportedVersion mapToVersion(Map<String, Object> map) {
    return ExportedVersion.builder()
        .version(toInteger(map.get("version")))
        .createdAt(parseDateTime(map.get("createdAt")))
        .createdBy((String) map.get("createdBy"))
        .config((String) map.get("config"))
        .build();
  }

  private Integer toInteger(Object obj) {
    if (obj == null) return null;
    if (obj instanceof Integer) return (Integer) obj;
    if (obj instanceof Number) return ((Number) obj).intValue();
    return Integer.parseInt(obj.toString());
  }

  private LocalDateTime parseDateTime(Object obj) {
    if (obj == null) return null;
    if (obj instanceof LocalDateTime) return (LocalDateTime) obj;
    if (obj instanceof String str) {
      try {
        return LocalDateTime.parse(str, DATE_TIME_FORMATTER);
      } catch (Exception e) {
        // Try ISO format
        try {
          return LocalDateTime.parse(str);
        } catch (Exception e2) {
          return null;
        }
      }
    }
    return null;
  }

  private ImportValidationResult validateConfig(ExportedConfig config) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (config == null) {
      return ImportValidationResult.error("Empty configuration");
    }

    if (config.getFormatVersion() == null) {
      errors.add("Missing format version");
    } else if (!config.getFormatVersion().startsWith("1.")) {
      errors.add("Unsupported format version: " + config.getFormatVersion());
    }

    if (config.getQueries() == null || config.getQueries().isEmpty()) {
      warnings.add("No queries to import");
    } else {
      Set<String> ids = new HashSet<>();
      for (ExportedQuery eq : config.getQueries()) {
        // Check for duplicates
        if (!ids.add(eq.getId())) {
          errors.add("Duplicate query ID: " + eq.getId());
        }

        // Validate required fields
        if (eq.getId() == null || eq.getId().isEmpty()) {
          errors.add("Query missing ID");
        }
        if (eq.getName() == null || eq.getName().isEmpty()) {
          errors.add("Query missing name: " + eq.getId());
        }
        if (eq.getQueryType() == null) {
          errors.add("Query missing type: " + eq.getName());
        }
        if (eq.getVersions() == null || eq.getVersions().isEmpty()) {
          errors.add("Query has no versions: " + eq.getName());
        }

        // Check existing queries for conflicts
        Optional<Query> existing = queryRepository.findById(eq.getId());
        if (existing.isPresent()) {
          Query query = existing.get();
          if (!query.getName().equals(eq.getName())) {
            warnings.add(
                "Query '"
                    + eq.getName()
                    + "' has different name in database: '"
                    + query.getName()
                    + "'");
          }
          if (eq.getCurrentVersion() <= query.getCurrentVersion()) {
            warnings.add(
                "Query '"
                    + eq.getName()
                    + "' version "
                    + eq.getCurrentVersion()
                    + " not newer than existing "
                    + query.getCurrentVersion());
          }
        }
      }
    }

    return new ImportValidationResult(
        errors.isEmpty(),
        errors,
        warnings,
        config.getQueries() != null ? config.getQueries().size() : 0);
  }

  private void createQuery(ExportedQuery eq, String importedBy) {
    Query query =
        Query.builder()
            .id(eq.getId())
            .name(eq.getName())
            .description(eq.getDescription())
            .category(eq.getCategory())
            .connectionName(eq.getConnectionName())
            .queryType(QueryType.valueOf(eq.getQueryType()))
            .currentVersion(eq.getCurrentVersion())
            .isActive(true)
            .createdBy(importedBy)
            .createdAt(LocalDateTime.now())
            .build();

    // Add all versions
    for (ExportedVersion ev : eq.getVersions()) {
      QueryVersion version =
          QueryVersion.builder()
              .id(UUID.randomUUID().toString())
              .query(query)
              .version(ev.getVersion())
              .configYaml(ev.getConfig())
              .createdAt(ev.getCreatedAt() != null ? ev.getCreatedAt() : LocalDateTime.now())
              .createdBy(ev.getCreatedBy() != null ? ev.getCreatedBy() : importedBy)
              .build();
      query.getVersions().add(version);
    }

    queryRepository.save(query);
  }

  private void updateQuery(Query query, ExportedQuery eq, String importedBy) {
    // Update metadata
    query.setName(eq.getName());
    query.setDescription(eq.getDescription());
    query.setCategory(eq.getCategory());
    query.setConnectionName(eq.getConnectionName());
    query.setQueryType(QueryType.valueOf(eq.getQueryType()));
    query.setUpdatedBy(importedBy);
    query.setUpdatedAt(LocalDateTime.now());

    // Add new versions
    int currentMax = query.getCurrentVersion();
    for (ExportedVersion ev : eq.getVersions()) {
      if (ev.getVersion() > currentMax) {
        QueryVersion version =
            QueryVersion.builder()
                .id(UUID.randomUUID().toString())
                .query(query)
                .version(ev.getVersion())
                .configYaml(ev.getConfig())
                .createdAt(ev.getCreatedAt() != null ? ev.getCreatedAt() : LocalDateTime.now())
                .createdBy(ev.getCreatedBy() != null ? ev.getCreatedBy() : importedBy)
                .build();
        versionRepository.save(version);
      }
    }

    query.setCurrentVersion(eq.getCurrentVersion());
    queryRepository.save(query);
  }

  /** Result of import validation. */
  public record ImportValidationResult(
      boolean valid, List<String> errors, List<String> warnings, int queryCount) {

    public static ImportValidationResult error(String message) {
      return new ImportValidationResult(false, List.of(message), List.of(), 0);
    }

    public boolean isValid() {
      return valid;
    }

    public List<String> getErrors() {
      return errors;
    }

    public List<String> getWarnings() {
      return warnings;
    }
  }

  /** Result of import operation. */
  public record ImportResult(
      boolean success, int created, int updated, int skipped, List<String> messages, String error) {

    public static ImportResult success(int created, int updated, int skipped, List<String> msgs) {
      return new ImportResult(true, created, updated, skipped, msgs, null);
    }

    public static ImportResult failure(String error) {
      return new ImportResult(false, 0, 0, 0, List.of(), error);
    }
  }
}
