package bio.terra.policy.common.model;

import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.model.ApiPolicyInput;
import bio.terra.policy.model.ApiPolicyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record PolicyInput(String namespace, String name, Map<String, String> additionalData) {
  public static PolicyInput fromApi(ApiPolicyInput apiInput) {
    // These nulls shouldn't happen.
    if (apiInput == null || apiInput.getNamespace() == null || apiInput.getName() == null) {
      throw new InvalidInputException("PolicyInput namespace and name cannot be null");
    }

    Map<String, String> data;
    if (apiInput.getAdditionalData() == null) {
      // Ensure we always have a map, even if it is empty.
      data = new HashMap<>();
    } else {
      data =
          apiInput.getAdditionalData().stream()
              .collect(Collectors.toMap(ApiPolicyPair::getKey, ApiPolicyPair::getValue));
    }
    return new PolicyInput(apiInput.getNamespace(), apiInput.getName(), data);
  }
}
