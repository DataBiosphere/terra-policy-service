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
      // Transaction management and DB retry configuration
      "bio.terra.common.retry.transaction",
      // Scan all policy service packages
      "bio.terra.policy",
    })
@EnableRetry
@EnableTransactionManagement
public class PolicySpringApplication {
  public static void main(String[] args) {
    // this system property is set so that there can be a policy-application.yml file instead of
    // application.yml since policy is still deployed as a library. We don't want the
    // application.yml in this library to conflict with any application.yml where it is imported.
    // Once tps is no longer a library this can be changed to plain application.yml as expected.
    System.setProperty("spring.config.name", "policy-application");

    new SpringApplicationBuilder(PolicySpringApplication.class)
        .initializers(new LoggingInitializer())
        .run(args);
  }
}
