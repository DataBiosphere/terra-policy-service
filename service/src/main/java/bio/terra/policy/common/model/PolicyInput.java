package bio.terra.policy.common.model;

import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.model.ApiPolicyInput;
import bio.terra.policy.model.ApiPolicyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PolicyInput {
  private final String namespace;
  private final String name;
  private final Map<String, String> additionalData;

  public PolicyInput(String namespace, String name, Map<String, String> additionalData) {
    this.namespace = namespace;
    this.name = name;
    this.additionalData = additionalData;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getAdditionalData() {
    return additionalData;
  }

  public ApiPolicyInput toApi() {
    List<ApiPolicyPair> apiPolicyPairs =
        additionalData.entrySet().stream()
            .map(e -> new ApiPolicyPair().key(e.getKey()).value(e.getValue()))
            .toList();

    return new ApiPolicyInput().namespace(namespace).name(name).additionalData(apiPolicyPairs);
  }

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
