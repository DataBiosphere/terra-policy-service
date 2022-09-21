package bio.terra.policy.app;

import bio.terra.tps.generated.model.VersionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TpsConfiguration {
  @Bean
  @ConfigurationProperties("tps.version")
  public VersionProperties getTpsVersion() {
    return new VersionProperties();
  }
}
