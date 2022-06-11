package bio.terra.policy.common.model;

import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.model.ApiPolicyInput;
import bio.terra.policy.model.ApiPolicyInputs;
import com.google.common.annotations.VisibleForTesting;
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

  public Map<String, PolicyInput> getInputs() {
    return inputs;
  }

  public ApiPolicyInputs toApi() {
    return new ApiPolicyInputs().inputs(inputs.values().stream().map(PolicyInput::toApi).toList());
  }

  /**
   * Convert API inputs to the internal working form. If the input is null or the input array is
   * empty, we return an empty map of inputs.
   *
   * @param apiInputs inputs from the API
   * @return inputs
   */
  public static PolicyInputs fromApi(@Nullable ApiPolicyInputs apiInputs) {
    if (apiInputs == null || apiInputs.getInputs() == null || apiInputs.getInputs().isEmpty()) {
      return new PolicyInputs(new HashMap<>());
    }

    var inputs = new HashMap<String, PolicyInput>();
    for (ApiPolicyInput apiInput : apiInputs.getInputs()) {
      // Convert the input so we get any errors before we process it further
      var input = PolicyInput.fromApi(apiInput);
      String key = composeKey(input.getNamespace(), input.getName());
      if (inputs.containsKey(key)) {
        throw new InvalidInputException("Duplicate policy attribute in policy input: " + key);
      }
      inputs.put(key, input);
    }
    return new PolicyInputs(inputs);
  }

  public static PolicyInputs fromDb(List<PolicyInput> inputList) {
    var inputs = new HashMap<String, PolicyInput>();
    for (PolicyInput input : inputList) {
      String key = composeKey(input.getNamespace(), input.getName());
      inputs.put(key, input);
    }
    return new PolicyInputs(inputs);
  }

  @VisibleForTesting
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
