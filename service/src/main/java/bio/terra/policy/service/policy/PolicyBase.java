package bio.terra.policy.service.policy;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;

public interface PolicyBase {
  PolicyName getPolicyName();

  PolicyInput combine(PolicyInput dependent, PolicyInput source);
}
