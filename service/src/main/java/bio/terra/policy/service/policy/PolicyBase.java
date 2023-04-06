package bio.terra.policy.service.policy;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;

public abstract class PolicyBase {
  public abstract PolicyName getPolicyName();

  /**
   * Combine two policies. Return null if there is a conflict
   *
   * @param dependent existing policy
   * @param source policy to combine in
   * @return resulting policy or null if there is a conflict
   */
  public final PolicyInput combine(PolicyInput dependent, PolicyInput source) {
    validatePolicyInput(dependent);
    validatePolicyInput(source);
    return performCombine(dependent, source);
  }

  protected abstract PolicyInput performCombine(PolicyInput dependent, PolicyInput source);

  /**
   * Remove a policy. In most cases, this results in the policy being gone.
   *
   * @param target existing policy
   * @param removePolicy policy to remove
   * @return resulting policy or null if the policy is gone
   */
  public final PolicyInput remove(PolicyInput target, PolicyInput removePolicy) {
    validatePolicyInput(target);
    validatePolicyInput(removePolicy);
    return performRemove(target, removePolicy);
  }

  protected abstract PolicyInput performRemove(PolicyInput target, PolicyInput removePolicy);

  /**
   * Validate a policy input.
   *
   * @param policyInput the input to validate
   * @return boolean indicating valid or not
   */
  public final boolean isValid(PolicyInput policyInput) {
    validatePolicyInput(policyInput);
    return performIsValid(policyInput);
  }

  protected abstract boolean performIsValid(PolicyInput policyInput);

  protected void validatePolicyInput(PolicyInput policyInput) {
    if (policyInput != null && !getPolicyName().equals(policyInput.getPolicyName())) {
      throw new InternalTpsErrorException(
          "This Policy instance can only be used with %s polices, %s was passed"
              .formatted(getPolicyName(), policyInput.getPolicyName()));
    }
  }
}
