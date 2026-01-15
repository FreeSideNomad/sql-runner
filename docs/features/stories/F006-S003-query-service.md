# F006-S003: Implement Query Service (CRUD + Versioning)

## User Story

**As a** developer
**I want** a service for query management
**So that** business logic is encapsulated

## Acceptance Criteria

- [ ] Given create query, then query and initial version saved
- [ ] Given update query, then new version created
- [ ] Given get query, then current version config returned
- [ ] Given delete query, then soft delete (is_active = false)
- [ ] Given list queries, then grouped by category

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/QueryService.java`
- `src/main/java/com/ivamare/dto/QueryDto.java`
- `src/main/java/com/ivamare/dto/CreateQueryRequest.java`
- `src/main/java/com/ivamare/dto/UpdateQueryRequest.java`

### Service Implementation
```java
@Service
@RequiredArgsConstructor
@Transactional
public class QueryService {
    private final QueryRepository queryRepository;
    private final QueryVersionRepository versionRepository;

    public Query createQuery(CreateQueryRequest request, String createdBy) {
        Query query = Query.builder()
            .id(UUID.randomUUID().toString())
            .name(request.getName())
            .description(request.getDescription())
            .category(request.getCategory())
            .connectionName(request.getConnectionName())
            .queryType(request.getQueryType())
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy(createdBy)
            .build();

        QueryVersion version = QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(query)
            .version(1)
            .configYaml(request.getConfigYaml())
            .createdAt(LocalDateTime.now())
            .createdBy(createdBy)
            .build();

        query.getVersions().add(version);
        return queryRepository.save(query);
    }

    public Query updateQuery(String id, UpdateQueryRequest request, String updatedBy) {
        Query query = queryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Query not found: " + id));

        // Create new version
        int newVersionNum = query.getCurrentVersion() + 1;
        QueryVersion version = QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(query)
            .version(newVersionNum)
            .configYaml(request.getConfigYaml())
            .createdAt(LocalDateTime.now())
            .createdBy(updatedBy)
            .build();

        versionRepository.save(version);

        query.setCurrentVersion(newVersionNum);
        query.setUpdatedAt(LocalDateTime.now());
        query.setUpdatedBy(updatedBy);

        return queryRepository.save(query);
    }

    public Map<String, List<QueryDto>> getQueriesGroupedByCategory() {
        return queryRepository.findByIsActiveTrue().stream()
            .map(QueryDto::from)
            .collect(Collectors.groupingBy(QueryDto::getCategory));
    }
}
```

## Test Plan

- [ ] Unit test: Create query creates version
- [ ] Unit test: Update query increments version
- [ ] Integration test: CRUD operations work end-to-end

## Parent Feature

Relates to F006-query-management
