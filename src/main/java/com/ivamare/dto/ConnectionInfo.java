package com.ivamare.dto;

import com.ivamare.config.ConnectionProperties.ConnectionConfig;
import com.ivamare.domain.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing connection information for display. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionInfo {
  private String id;
  private String name;
  private DatabaseType type;
  private String host;
  private int port;
  private String database;
  private String schema;
  private boolean connected;
  private String errorMessage;

  /** Create ConnectionInfo from config entry. */
  public static ConnectionInfo from(String id, ConnectionConfig config) {
    return ConnectionInfo.builder()
        .id(id)
        .name(config.getName())
        .type(config.getType())
        .host(config.getHost())
        .port(config.getEffectivePort())
        .database(config.getDatabase())
        .schema(config.getSchema())
        .connected(false)
        .build();
  }
}
