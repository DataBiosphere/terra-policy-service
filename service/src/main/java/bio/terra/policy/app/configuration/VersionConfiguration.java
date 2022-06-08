package bio.terra.policy.app.configuration;

import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Read from the git.properties file auto-generated at build time */
@Configuration
@ConfigurationProperties("policy.version")
@PropertySource("git.properties")
public class VersionConfiguration implements InitializingBean {
  private final ConfigurableEnvironment configurableEnvironment;
  private String gitHash;
  private String gitTag;
  private String build;
  private String github;

  @Autowired
  public VersionConfiguration(ConfigurableEnvironment configurableEnvironment) {
    this.configurableEnvironment = configurableEnvironment;
  }

  public String getGitHash() {
    return gitHash;
  }

  public void setGitHash(String gitHash) {
    this.gitHash = gitHash;
  }

  public String getGitTag() {
    return gitTag;
  }

  public void setGitTag(String gitTag) {
    this.gitTag = gitTag;
  }

  public String getBuild() {
    return build;
  }

  public void setBuild(String build) {
    this.build = build;
  }

  public String getGithub() {
    return github;
  }

  public void setGithub(String github) {
    this.github = github;
  }

  /**
   * Copies the version.build property to spring.application.version, for consumption by the common
   * logging module's JSON layout.
   */
  @Override
  public void afterPropertiesSet() {
    configurableEnvironment
        .getPropertySources()
        .addFirst(
            new MapPropertySource("version", Map.of("spring.application.version", getBuild())));
  }
}
