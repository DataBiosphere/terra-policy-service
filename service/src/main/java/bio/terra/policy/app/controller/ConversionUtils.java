package bio.terra.policy.app.controller;

import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.generated.model.ApiTpsPaoConflict;
import bio.terra.policy.generated.model.ApiTpsPaoDescription;
import bio.terra.policy.generated.model.ApiTpsPaoGetResult;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateResult;
import bio.terra.policy.generated.model.ApiTpsPolicyInput;
import bio.terra.policy.generated.model.ApiTpsPolicyInputs;
import bio.terra.policy.generated.model.ApiTpsPolicyPair;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.policy.model.PolicyUpdateResult;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

/** Static methods to convert from API to internal objects and back. */
public class ConversionUtils {
  static PolicyInput policyInputFromApi(ApiTpsPolicyInput apiInput) {
    // These nulls shouldn't happen.
    if (apiInput == null
        || StringUtils.isEmpty(apiInput.getNamespace())
        || StringUtils.isEmpty(apiInput.getName())) {
      throw new InvalidInputException("PolicyInput namespace and name cannot be null");
    }

    Multimap<String, String> data = ArrayListMultimap.create();
    if (apiInput.getAdditionalData() != null) {
      apiInput.getAdditionalData().forEach(item -> data.put(item.getKey(), item.getValue()));
    }

    return new PolicyInput(new PolicyName(apiInput.getNamespace(), apiInput.getName()), data);
  }

  static ApiTpsPolicyInput policyInputToApi(PolicyInput input) {
    List<ApiTpsPolicyPair> apiPolicyPairs =
        input.getAdditionalData().entries().stream()
            .map(e -> new ApiTpsPolicyPair().key(e.getKey()).value(e.getValue()))
            .toList();

    final PolicyName policyName = input.getPolicyName();
    return new ApiTpsPolicyInput()
        .namespace(policyName.getNamespace())
        .name(policyName.getName())
        .additionalData(apiPolicyPairs);
  }

  static PolicyInputs policyInputsFromApi(@Nullable ApiTpsPolicyInputs apiInputs) {
    if (apiInputs == null || apiInputs.getInputs() == null || apiInputs.getInputs().isEmpty()) {
      return new PolicyInputs(new HashMap<>());
    }

    var inputs = new HashMap<String, PolicyInput>();
    for (ApiTpsPolicyInput apiInput : apiInputs.getInputs()) {
      // Convert the input so we get any errors before we process it further
      var input = policyInputFromApi(apiInput);
      String key = input.getKey();
      if (inputs.containsKey(key)) {
        throw new InvalidInputException("Duplicate policy attribute in policy input: " + key);
      }
      inputs.put(key, input);
    }
    return new PolicyInputs(inputs);
  }

  static ApiTpsPolicyInputs policyInputsToApi(PolicyInputs inputs) {

    // old policies could have been created with a null list.
    if (inputs == null) {
      return new ApiTpsPolicyInputs();
    }

    return new ApiTpsPolicyInputs()
        .inputs(
            inputs.getInputs().values().stream().map(ConversionUtils::policyInputToApi).toList());
  }

  static ApiTpsPaoGetResult paoToApi(Pao pao) {
    return new ApiTpsPaoGetResult()
        .objectId(pao.getObjectId())
        .component(pao.getComponent().toApi())
        .objectType(pao.getObjectType().toApi())
        .attributes(policyInputsToApi(pao.getAttributes()))
        .effectiveAttributes(policyInputsToApi(pao.getEffectiveAttributes()))
        .sourcesObjectIds(pao.getSourceObjectIds().stream().toList())
        .deleted((pao.getDeleted()));
  }

  static ApiTpsPaoUpdateResult updateResultToApi(PolicyUpdateResult result) {
    ApiTpsPaoUpdateResult apiResult =
        new ApiTpsPaoUpdateResult()
            .updateApplied(result.updateApplied())
            .resultingPao(paoToApi(result.computedPao()));

    for (PolicyConflict conflict : result.conflicts()) {
      var apiConflict =
          new ApiTpsPaoConflict()
              .namespace(conflict.policyName().getNamespace())
              .name(conflict.policyName().getName())
              .targetPao(paoToApiPaoDescription(conflict.pao()))
              .conflictPao(paoToApiPaoDescription(conflict.conflictPao()));
      apiResult.addConflictsItem(apiConflict);
    }

    return apiResult;
  }

  static ApiTpsPaoDescription paoToApiPaoDescription(Pao pao) {
    return new ApiTpsPaoDescription()
        .objectId(pao.getObjectId())
        .component(pao.getComponent().toApi())
        .objectType(pao.getObjectType().toApi());
  }
}
