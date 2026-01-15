package com.ivamare.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Configuration properties for SQL Runner security settings. */
@Configuration
@ConfigurationProperties(prefix = "sqlrunner.security")
@Data
public class SecurityProperties {

  /** Mapping of application roles to AD group DNs. */
  private Map<String, String> roleMapping = new HashMap<>();
}
