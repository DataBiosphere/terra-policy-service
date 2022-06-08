package bio.terra.policy.service.policy.model;

import bio.terra.policy.model.ApiPolicyInputs;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PolicyInputs provides a map of inputs. The key is composed as input.namespace + ":" + input.name.
 */
public record PolicyInputs(Map<String, PolicyInput> inputs) {
  public static PolicyInputs fromApi(ApiPolicyInputs apiInputs) {
    Map<String, PolicyInput> inputs =
        apiInputs.getInputs().stream()
            .collect(
                Collectors.toMap(
                    input -> input.getNamespace() + ":" + input.getName(), PolicyInput::fromApi));
    return new PolicyInputs(inputs);
  }
}
