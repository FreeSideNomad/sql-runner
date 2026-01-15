package com.ivamare.dto;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for displaying query information. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryDto {
  private String id;
  private String name;
  private String description;
  private String category;
  private String connectionName;
  private QueryType queryType;
  private Integer currentVersion;
  private LocalDateTime createdAt;
  private String createdBy;
  private LocalDateTime updatedAt;
  private String updatedBy;

  /** Create QueryDto from Query entity. */
  public static QueryDto from(Query query) {
    return QueryDto.builder()
        .id(query.getId())
        .name(query.getName())
        .description(query.getDescription())
        .category(query.getCategory())
        .connectionName(query.getConnectionName())
        .queryType(query.getQueryType())
        .currentVersion(query.getCurrentVersion())
        .createdAt(query.getCreatedAt())
        .createdBy(query.getCreatedBy())
        .updatedAt(query.getUpdatedAt())
        .updatedBy(query.getUpdatedBy())
        .build();
  }
}
