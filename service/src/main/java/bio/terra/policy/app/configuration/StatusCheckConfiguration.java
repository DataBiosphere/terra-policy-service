package bio.terra.policy.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "policy.status-check")
public record StatusCheckConfiguration(
    boolean enabled,
    int pollingIntervalSeconds,
    int startupWaitSeconds,
    int stalenessThresholdSeconds) {}
