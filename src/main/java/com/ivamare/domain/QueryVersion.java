package com.ivamare.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Entity representing a version of a query configuration. */
@Entity
@Table(name = "query_versions", schema = "sqlrunner")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryVersion {

  @Id
  @Column(length = 36)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "query_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Query query;

  @Column(nullable = false)
  private Integer version;

  @Column(name = "config_yaml", nullable = false, columnDefinition = "TEXT")
  private String configYaml;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "created_by", nullable = false, length = 100)
  private String createdBy;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
