# F006-S002: Create QueryVersion Entity and Repository

## User Story

**As a** developer
**I want** a QueryVersion entity
**So that** query configuration history is preserved

## Acceptance Criteria

- [ ] Given QueryVersion entity, then version number tracked
- [ ] Given QueryVersion entity, then config_yaml stores full YAML
- [ ] Given QueryVersion entity, then relationship to Query defined
- [ ] Given QueryVersion repository, then find by query and version works
- [ ] Given new version, then version number auto-incremented

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/domain/QueryVersion.java`
- `src/main/java/com/ivamare/repository/QueryVersionRepository.java`

### QueryVersion Entity
```java
@Entity
@Table(name = "query_versions", schema = "sqlrunner")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryVersion {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "config_yaml", nullable = false, columnDefinition = "TEXT")
    private String configYaml;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
}
```

### Repository
```java
public interface QueryVersionRepository extends JpaRepository<QueryVersion, String> {
    Optional<QueryVersion> findByQueryIdAndVersion(String queryId, Integer version);
    List<QueryVersion> findByQueryIdOrderByVersionDesc(String queryId);
    Optional<QueryVersion> findTopByQueryIdOrderByVersionDesc(String queryId);
}
```

### Version Creation Logic
```java
public QueryVersion createNewVersion(Query query, String configYaml, String createdBy) {
    int nextVersion = queryVersionRepository.findTopByQueryIdOrderByVersionDesc(query.getId())
        .map(v -> v.getVersion() + 1)
        .orElse(1);

    return QueryVersion.builder()
        .id(UUID.randomUUID().toString())
        .query(query)
        .version(nextVersion)
        .configYaml(configYaml)
        .createdAt(LocalDateTime.now())
        .createdBy(createdBy)
        .build();
}
```

## Test Plan

- [ ] Integration test: Version created with query
- [ ] Integration test: Version number increments
- [ ] Integration test: Find by query and version works

## Parent Feature

Relates to F006-query-management
