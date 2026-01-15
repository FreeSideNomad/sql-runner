# F006-S001: Create Query Entity and Repository

## User Story

**As a** developer
**I want** a Query entity with JPA mapping
**So that** query templates can be persisted

## Acceptance Criteria

- [ ] Given Query entity, then all fields per schema mapped
- [ ] Given Query repository, then CRUD operations available
- [ ] Given Query entity, then category field for grouping
- [ ] Given Query entity, then query_type enum (SELECT, UPDATE_WORKFLOW)
- [ ] Given Query entity, then audit fields populated

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/domain/Query.java` (extend from F002-S006)
- `src/main/java/com/ivamare/repository/QueryRepository.java`

### Query Entity
```java
@Entity
@Table(name = "queries", schema = "sqlrunner")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Query {
    @Id
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "connection_name", nullable = false, length = 100)
    private String connectionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_type", nullable = false, length = 20)
    private QueryType queryType;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QueryVersion> versions = new ArrayList<>();

    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
```

### Repository
```java
public interface QueryRepository extends JpaRepository<Query, String> {
    List<Query> findByCategory(String category);
    List<Query> findByIsActiveTrue();
    List<Query> findByCategoryOrderByNameAsc(String category);

    @Query("SELECT DISTINCT q.category FROM Query q ORDER BY q.category")
    List<String> findDistinctCategories();
}
```

## Test Plan

- [ ] Integration test: CRUD operations work
- [ ] Integration test: Find by category works
- [ ] Integration test: Distinct categories query works

## Parent Feature

Relates to F006-query-management
