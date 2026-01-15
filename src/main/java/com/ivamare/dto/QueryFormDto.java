package com.ivamare.dto;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for query create/edit form binding. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryFormDto {

  private String id;

  @NotBlank(message = "Name is required")
  @Size(max = 200, message = "Name must be at most 200 characters")
  private String name;

  @Size(max = 1000, message = "Description must be at most 1000 characters")
  private String description;

  @NotBlank(message = "Category is required")
  @Size(max = 100, message = "Category must be at most 100 characters")
  private String category;

  @NotBlank(message = "Connection is required")
  @Size(max = 100, message = "Connection name must be at most 100 characters")
  private String connectionName;

  @NotNull(message = "Query type is required")
  private QueryType queryType;

  // Structured config fields
  @Builder.Default private QueryConfigFormDto config = new QueryConfigFormDto();

  /** Create form DTO from Query entity and current version config. */
  public static QueryFormDto from(Query query, QueryConfig parsedConfig) {
    return QueryFormDto.builder()
        .id(query.getId())
        .name(query.getName())
        .description(query.getDescription())
        .category(query.getCategory())
        .connectionName(query.getConnectionName())
        .queryType(query.getQueryType())
        .config(QueryConfigFormDto.from(parsedConfig))
        .build();
  }

  /** Check if this is an edit (existing query) or create (new query). */
  public boolean isEdit() {
    return id != null && !id.isEmpty();
  }

  /** Ensure config and parameters list are initialized. */
  public void ensureConfigInitialized() {
    if (config == null) {
      config = new QueryConfigFormDto();
    }
    if (config.getParameters() == null) {
      config.setParameters(new ArrayList<>());
    }
  }
}
