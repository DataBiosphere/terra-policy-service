package bio.terra.policy.common.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * PolicyInputs provides a map of inputs. The key is composed as input.namespace + ":" + input.name.
 */
public class PolicyInputs {
  private final Map<String, PolicyInput> inputs;

  public PolicyInputs(Map<String, PolicyInput> inputs) {
    this.inputs = inputs;
  }

  public Map<String, PolicyInput> getInputs() {
    return inputs;
  }

  public static PolicyInputs fromDb(List<PolicyInput> inputList) {
    var inputs = new HashMap<String, PolicyInput>();
    for (PolicyInput input : inputList) {
      String key = composeKey(input.getNamespace(), input.getName());
      inputs.put(key, input);
    }
    return new PolicyInputs(inputs);
  }

  public static String composeKey(String namespace, String name) {
    return namespace + ":" + name;
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
