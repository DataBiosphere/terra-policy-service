package bio.terra.policy.service.policy;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;

/** A policy that does not contain any additional data. */
public class PolicyLabel implements PolicyBase {
  private final PolicyName policyName;

  public PolicyLabel(PolicyName policyName) {
    this.policyName = policyName;
  }

  @Override
  public PolicyName getPolicyName() {
    return policyName;
  }

  /**
   * Return either dependent or source, whichever is non-null, or null if both are null.
   *
   * @param dependent policy input
   * @param source policy input
   * @return policy input or null, if there is a conflict
   */
  @Override
  public PolicyInput combine(PolicyInput dependent, PolicyInput source) {
    return dependent == null ? source : dependent;
  }

  /**
   * Always return null, meaning the policy is deleted.
   *
   * @param target existing policy
   * @param removePolicy policy to remove
   * @return null
   */
  @Override
  public PolicyInput remove(PolicyInput target, PolicyInput removePolicy) {
    return null;
  }

  /**
   * Label policies cannot have additional data.
   *
   * @param policyInput the input to validate
   * @return true if additional data is empty
   */
  @Override
  public boolean isValid(PolicyInput policyInput) {
    return policyInput.getAdditionalData().isEmpty();
  }
}
