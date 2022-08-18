package bio.terra.policy.service.policy;

import bio.terra.policy.common.model.PolicyName;
import java.util.Optional;

public enum PolicyKnownPolicy {
  GROUP_CONSTRAINT(new PolicyGroupConstraint());

  private final PolicyBase policy;

  PolicyKnownPolicy(PolicyBase policy) {
    this.policy = policy;
  }

  public PolicyBase getPolicy() {
    return policy;
  }

  /**
   * Given a policy name, see if it is a known policy. If so, return its PolicyBase. Otherwise,
   * return null.
   *
   * @param policyName name of the policy we are looking for
   * @return a policy object or null ifn ot a known policy
   */
  public static Optional<PolicyBase> findPolicyBaseByName(PolicyName policyName) {
    for (PolicyKnownPolicy policyCombiner : values()) {
      if (policyCombiner.getPolicy().getPolicyName().equals(policyName)) {
        return Optional.of(policyCombiner.getPolicy());
      }
    }

    return Optional.empty();
  }
}
