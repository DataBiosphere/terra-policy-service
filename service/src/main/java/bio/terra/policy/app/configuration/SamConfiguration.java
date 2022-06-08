package bio.terra.policy.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "policy.sam")
public record SamConfiguration(String basePath, String resourceId) {}
