package bio.terra.policy.common.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PolicyInput {
  private final PolicyName policyName;
  private final Multimap<String, String> additionalData;
  private final Set<UUID> conflicts;

  public PolicyInput(
      PolicyName policyName, Multimap<String, String> additionalData, Set<UUID> conflicts) {
    this.policyName = policyName;
    this.additionalData = additionalData;
    this.conflicts = conflicts;
  }

  public PolicyInput(PolicyName policyName, Multimap<String, String> additionalData) {
    this(policyName, additionalData, new HashSet<>());
  }

  public PolicyInput(String namespace, String name, Multimap<String, String> additionalData) {
    this(new PolicyName(namespace, name), additionalData, new HashSet<>());
  }

  // Handy constructor for the non-multimap uses
  public static PolicyInput createFromMap(
      String namespace, String name, Map<String, String> additionalData) {
    // Convert the map to a multimap
    Multimap<String, String> mm = ArrayListMultimap.create();
    additionalData.entrySet().forEach(e -> mm.put(e.getKey(), e.getValue()));
    return new PolicyInput(new PolicyName(namespace, name), mm, new HashSet<>());
  }

  public PolicyName getPolicyName() {
    return policyName;
  }

  public String getPolicyNameKey() {
    return policyName.getKey();
  }

  public String getKey() {
    return policyName.getKey();
  }

  public Collection<String> getData(String key) {
    return additionalData.get(key);
  }

  public Multimap<String, String> getAdditionalData() {
    return additionalData;
  }

  public Set<UUID> getConflicts() {
    return conflicts;
  }

  public PolicyInput duplicateWithoutConflicts() {
    return new PolicyInput(policyName, additionalData);
  }
}
