package com.ivamare.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/** Entity representing a query template. */
@Entity
@Table(name = "queries", schema = "sqlrunner")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Query {

  @Id
  @Column(length = 36)
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
  @Builder.Default
  private Integer currentVersion = 1;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "created_by", nullable = false, length = 100)
  private String createdBy;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "updated_by", length = 100)
  private String updatedBy;

  @OneToMany(
      mappedBy = "query",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Builder.Default
  private List<QueryVersion> versions = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
