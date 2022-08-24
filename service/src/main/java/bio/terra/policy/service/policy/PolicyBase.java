package bio.terra.policy.service.policy;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;

public interface PolicyBase {
  PolicyName getPolicyName();

  /**
   * Combine two policies. Return null if there is a conflict
   *
   * @param dependent existing policy
   * @param source policy to combine in
   * @return resulting policy or null if there is a conflict
   */
  PolicyInput combine(PolicyInput dependent, PolicyInput source);

  /**
   * Remove a policy. In most cases, this results in the policy being gone.
   *
   * @param target existing policy
   * @param removePolicy policy to remove
   * @return resulting policy or null if the policy is gone
   */
  PolicyInput remove(PolicyInput target, PolicyInput removePolicy);
}
