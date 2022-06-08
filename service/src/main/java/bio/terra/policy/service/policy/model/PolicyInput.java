package bio.terra.policy.service.policy.model;

import bio.terra.policy.model.ApiPolicyInput;
import bio.terra.policy.model.ApiPolicyPair;
import java.util.Map;
import java.util.stream.Collectors;

public record PolicyInput(String namespace, String name, Map<String, String> additionalData) {
  public static PolicyInput fromApi(ApiPolicyInput apiInput) {
    Map<String, String> data =
        apiInput.getAdditionalData().stream()
            .collect(Collectors.toMap(ApiPolicyPair::getKey, ApiPolicyPair::getValue));
    return new PolicyInput(apiInput.getNamespace(), apiInput.getNamespace(), data);
  }
}
