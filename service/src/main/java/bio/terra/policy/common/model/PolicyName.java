package bio.terra.policy.common.model;

import java.util.Objects;

public class PolicyName {
  private final String namespace;
  private final String name;

  public PolicyName(String namespace, String name) {
    this.namespace = namespace;
    this.name = name;
  }

  public String getKey() {
    return composeKey(namespace, name);
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public static String composeKey(String namespace, String name) {
    return namespace + ":" + name;
  }

  @Override
  public String toString() {
    return getKey();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PolicyName)) return false;
    PolicyName that = (PolicyName) o;
    return Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, name);
  }
}
