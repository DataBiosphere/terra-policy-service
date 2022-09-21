package bio.terra.policy.app;

import bio.terra.policy.generated.model.VersionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PolicySpringConfiguration {
  @Bean
  @ConfigurationProperties("tps.version")
  public VersionProperties getTpsVersion() {
    return new VersionProperties();
  }
}
