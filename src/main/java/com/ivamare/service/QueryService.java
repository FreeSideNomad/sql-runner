package com.ivamare.service;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryVersion;
import com.ivamare.dto.QueryConfig;
import com.ivamare.dto.QueryDto;
import com.ivamare.dto.QueryFormDto;
import com.ivamare.repository.QueryRepository;
import com.ivamare.repository.QueryVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing query templates with versioning. */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

  private final QueryRepository queryRepository;
  private final QueryVersionRepository versionRepository;
  private final ConfigYamlService configYamlService;

  /**
   * Get all active queries grouped by category.
   *
   * @return Map of category name to list of queries in that category
   */
  @Transactional(readOnly = true)
  public Map<String, List<QueryDto>> getQueriesGroupedByCategory() {
    return queryRepository.findByIsActiveTrueOrderByNameAsc().stream()
        .map(QueryDto::from)
        .collect(Collectors.groupingBy(QueryDto::getCategory));
  }

  /**
   * Get all active queries sorted by name.
   *
   * @return List of queries sorted by name
   */
  @Transactional(readOnly = true)
  public List<QueryDto> getAllQueriesSortedByName() {
    return queryRepository.findByIsActiveTrueOrderByNameAsc().stream().map(QueryDto::from).toList();
  }

  /**
   * Get all active queries grouped by connection name, then by category, with queries sorted by
   * name within each category.
   *
   * @return Nested map: Connection -> Category -> List of queries (sorted by name)
   */
  @Transactional(readOnly = true)
  public Map<String, Map<String, List<QueryDto>>> getQueriesGroupedByConnectionAndCategory() {
    List<QueryDto> allQueries =
        queryRepository.findByIsActiveTrueOrderByNameAsc().stream().map(QueryDto::from).toList();

    // Group by connection, then by category, maintaining sort order
    Map<String, Map<String, List<QueryDto>>> result = new TreeMap<>();

    for (QueryDto query : allQueries) {
      String connection = query.getConnectionName() != null ? query.getConnectionName() : "Unknown";
      String category = query.getCategory() != null ? query.getCategory() : "Uncategorized";

      result
          .computeIfAbsent(connection, k -> new TreeMap<>())
          .computeIfAbsent(category, k -> new java.util.ArrayList<>())
          .add(query);
    }

    // Sort queries within each category by name (already sorted from DB, but ensure consistency)
    for (Map<String, List<QueryDto>> categoryMap : result.values()) {
      for (List<QueryDto> queries : categoryMap.values()) {
        queries.sort(Comparator.comparing(QueryDto::getName, String.CASE_INSENSITIVE_ORDER));
      }
    }

    return result;
  }

  /**
   * Get all distinct categories.
   *
   * @return List of category names
   */
  @Transactional(readOnly = true)
  public List<String> getAllCategories() {
    return queryRepository.findDistinctCategories();
  }

  /**
   * Get a query by ID.
   *
   * @param id Query ID
   * @return Query entity
   * @throws EntityNotFoundException if query not found
   */
  @Transactional(readOnly = true)
  public Query getQuery(String id) {
    return queryRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Query not found: " + id));
  }

  /**
   * Get query as DTO.
   *
   * @param id Query ID
   * @return QueryDto
   */
  @Transactional(readOnly = true)
  public QueryDto getQueryDto(String id) {
    return QueryDto.from(getQuery(id));
  }

  /**
   * Get query form DTO for editing, including current version config.
   *
   * @param id Query ID
   * @return QueryFormDto with current configuration parsed into structured form
   */
  @Transactional(readOnly = true)
  public QueryFormDto getQueryForEdit(String id) {
    Query query = getQuery(id);
    QueryVersion currentVersion =
        versionRepository
            .findByQueryIdAndVersion(id, query.getCurrentVersion())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Version not found for query: " + id + " v" + query.getCurrentVersion()));
    QueryConfig parsedConfig = configYamlService.parse(currentVersion.getConfigYaml());
    return QueryFormDto.from(query, parsedConfig);
  }

  /**
   * Get the current config YAML for a query.
   *
   * @param id Query ID
   * @return Config YAML string
   */
  @Transactional(readOnly = true)
  public String getCurrentConfigYaml(String id) {
    Query query = getQuery(id);
    return versionRepository
        .findByQueryIdAndVersion(id, query.getCurrentVersion())
        .map(QueryVersion::getConfigYaml)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Version not found for query: " + id + " v" + query.getCurrentVersion()));
  }

  /**
   * Create a new query with initial version.
   *
   * @param form Form data
   * @param createdBy Username creating the query
   * @return Created query
   */
  @Transactional
  public Query createQuery(QueryFormDto form, String createdBy) {
    log.info("Creating query '{}' by user '{}'", form.getName(), createdBy);

    // Convert form config to YAML
    String configYaml = configYamlService.toYaml(form.getConfig().toQueryConfig());

    Query query =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name(form.getName())
            .description(form.getDescription())
            .category(form.getCategory())
            .connectionName(form.getConnectionName())
            .queryType(form.getQueryType())
            .currentVersion(1)
            .isActive(true)
            .createdBy(createdBy)
            .build();

    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(query)
            .version(1)
            .configYaml(configYaml)
            .createdBy(createdBy)
            .build();

    query.getVersions().add(version);
    Query saved = queryRepository.save(query);

    log.info("Created query '{}' with ID '{}'", saved.getName(), saved.getId());
    return saved;
  }

  /**
   * Update an existing query, creating a new version.
   *
   * @param id Query ID
   * @param form Form data
   * @param updatedBy Username updating the query
   * @return Updated query
   */
  @Transactional
  public Query updateQuery(String id, QueryFormDto form, String updatedBy) {
    log.info("Updating query '{}' by user '{}'", id, updatedBy);

    Query query = getQuery(id);

    // Convert form config to YAML
    String configYaml = configYamlService.toYaml(form.getConfig().toQueryConfig());

    // Update metadata
    query.setName(form.getName());
    query.setDescription(form.getDescription());
    query.setCategory(form.getCategory());
    query.setConnectionName(form.getConnectionName());
    query.setQueryType(form.getQueryType());
    query.setUpdatedBy(updatedBy);
    query.setUpdatedAt(LocalDateTime.now());

    // Create new version
    int newVersionNum = query.getCurrentVersion() + 1;
    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(query)
            .version(newVersionNum)
            .configYaml(configYaml)
            .createdAt(LocalDateTime.now())
            .createdBy(updatedBy)
            .build();

    versionRepository.save(version);
    query.setCurrentVersion(newVersionNum);

    Query saved = queryRepository.save(query);
    log.info("Updated query '{}' to version {}", saved.getName(), newVersionNum);
    return saved;
  }

  /**
   * Soft delete a query (set is_active = false).
   *
   * @param id Query ID
   * @param deletedBy Username deleting the query
   */
  @Transactional
  public void deleteQuery(String id, String deletedBy) {
    log.info("Deleting query '{}' by user '{}'", id, deletedBy);

    Query query = getQuery(id);
    query.setIsActive(false);
    query.setUpdatedBy(deletedBy);
    query.setUpdatedAt(LocalDateTime.now());

    queryRepository.save(query);
    log.info("Soft deleted query '{}'", id);
  }

  /**
   * Get version history for a query.
   *
   * @param queryId Query ID
   * @return List of versions, newest first
   */
  @Transactional(readOnly = true)
  public List<QueryVersion> getVersionHistory(String queryId) {
    // Verify query exists
    getQuery(queryId);
    return versionRepository.findByQueryIdOrderByVersionDesc(queryId);
  }

  /**
   * Get a specific version of a query.
   *
   * @param queryId Query ID
   * @param versionNum Version number
   * @return QueryVersion
   */
  @Transactional(readOnly = true)
  public QueryVersion getVersion(String queryId, int versionNum) {
    return versionRepository
        .findByQueryIdAndVersion(queryId, versionNum)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Version " + versionNum + " not found for query: " + queryId));
  }

  /**
   * Check if a query name already exists.
   *
   * @param name Query name
   * @return true if name exists
   */
  @Transactional(readOnly = true)
  public boolean queryNameExists(String name) {
    return queryRepository.existsByNameAndIsActiveTrue(name);
  }
}
