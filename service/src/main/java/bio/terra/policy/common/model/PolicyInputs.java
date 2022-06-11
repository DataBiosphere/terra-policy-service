package bio.terra.policy.common.model;

import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.model.ApiPolicyInput;
import bio.terra.policy.model.ApiPolicyInputs;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * PolicyInputs provides a map of inputs. The key is composed as input.namespace + ":" + input.name.
 */
public record PolicyInputs(Map<String, PolicyInput> inputs) {
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
      String key = composeKey(input.namespace(), input.name());
      if (inputs.containsKey(key)) {
        throw new InvalidInputException("Duplicate policy attribute in policy input: " + key);
      }
      inputs.put(key, input);
    }
    return new PolicyInputs(inputs);
  }

  public static String composeKey(String namespace, String name) {
    return namespace + ":" + name;
  }
}
