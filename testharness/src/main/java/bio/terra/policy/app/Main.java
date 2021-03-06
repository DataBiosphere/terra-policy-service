package bio.terra.policy.app;

import bio.terra.common.logging.LoggingInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
    exclude = {
      // We don't make use of DataSource in this application, so exclude it from scanning.
      DataSourceAutoConfiguration.class,
    })
@ComponentScan(
    basePackages = {
      // Scan for logging-related components & configs
      "bio.terra.common.logging",
      // Scan for Liquibase migration components & configs
      "bio.terra.common.migrate",
      // Scan all policy service packages
      "bio.terra.policy",
    })
@EnableRetry
@EnableTransactionManagement
public class Main {
  public static void main(String[] args) {
    new SpringApplicationBuilder(Main.class).initializers(new LoggingInitializer()).run(args);
  }
}
