package com.ivamare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SqlRunnerApplication {

  public static void main(String[] args) {
    SpringApplication.run(SqlRunnerApplication.class, args);
  }
}
