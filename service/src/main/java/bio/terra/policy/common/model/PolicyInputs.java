package bio.terra.policy.common.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
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

  public @Nullable PolicyInput lookupPolicy(PolicyName name) {
    return inputs.get(name.getKey());
  }

  public Map<String, PolicyInput> getInputs() {
    return inputs;
  }

  public void removeInput(PolicyInput removeInput) {
    inputs.remove(removeInput.getKey());
  }

  public static PolicyInputs fromDb(List<PolicyInput> inputList) {
    PolicyInputs inputs = new PolicyInputs();
    for (PolicyInput input : inputList) {
      inputs.addInput(input);
    }
    return inputs;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", PolicyInputs.class.getSimpleName() + "[", "]")
        .add("inputs=" + inputs)
        .toString();
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
