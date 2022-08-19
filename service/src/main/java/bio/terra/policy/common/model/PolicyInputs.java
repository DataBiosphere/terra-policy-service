package bio.terra.policy.common.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * PolicyInputs provides a map of inputs. The key is composed as input.namespace + ":" + input.name.
 */
public class PolicyInputs {
  private final Map<String, PolicyInput> inputs;

  public PolicyInputs(Map<String, PolicyInput> inputs) {
    this.inputs = inputs;
  }

  public PolicyInputs() {
    this.inputs = new HashMap<>();
  }

  public void addInput(PolicyInput input) {
    inputs.put(input.getKey(), input);
  }

  /**
   * Lookup a matching policy input given an incoming policy input
   *
   * @param input incoming policy input
   * @return matching policy input or null if not found
   */
  public @Nullable PolicyInput lookupPolicy(PolicyInput input) {
    return inputs.get(input.getKey());
  }

  public Map<String, PolicyInput> getInputs() {
    return inputs;
  }

  public static PolicyInputs fromDb(List<PolicyInput> inputList) {
    PolicyInputs inputs = new PolicyInputs();
    for (PolicyInput input : inputList) {
      inputs.addInput(input);
    }
    return inputs;
  }

  public PolicyInputs duplicateWithoutConflicts() {
    Map<String, PolicyInput> dupMap = new HashMap<>();
    for (var entry : inputs.entrySet()) {
      var dupInput = entry.getValue().duplicateWithoutConflicts();
      dupMap.put(entry.getKey(), dupInput);
    }
    return new PolicyInputs(dupMap);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PolicyInputs)) return false;
    PolicyInputs that = (PolicyInputs) o;
    return Objects.equals(inputs, that.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputs);
  }
}
