package bio.terra.policy.common.model;

import java.util.Map;

public class PolicyInput {
  private final String namespace;
  private final String name;
  private final Map<String, String> additionalData;

  public PolicyInput(String namespace, String name, Map<String, String> additionalData) {
    this.namespace = namespace;
    this.name = name;
    this.additionalData = additionalData;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getAdditionalData() {
    return additionalData;
  }
}
