package com.ivamare.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Root DTO for exported configuration YAML. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportedConfig {

  private String formatVersion;
  private LocalDateTime exportedAt;
  private String exportedBy;
  private List<ExportedQuery> queries;

  /** Exported query metadata. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ExportedQuery {
    private String id;
    private String name;
    private String description;
    private String category;
    private String connectionName;
    private String queryType;
    private Integer currentVersion;
    private List<ExportedVersion> versions;
  }

  /** Exported query version. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ExportedVersion {
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
    private String config;
  }
}
