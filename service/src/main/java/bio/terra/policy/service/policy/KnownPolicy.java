package bio.terra.policy.service.policy;

import bio.terra.policy.common.model.PolicyName;
import java.util.Optional;

public enum KnownPolicy {
  GROUP_CONSTRAINT(new PolicyGroupConstraint()),
  REGION_CONSTRAINT(new PolicyRegionConstraint());

  private final PolicyBase policy;

  KnownPolicy(PolicyBase policy) {
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
    for (KnownPolicy policyCombiner : values()) {
      if (policyCombiner.getPolicy().getPolicyName().equals(policyName)) {
        return Optional.of(policyCombiner.getPolicy());
      }
    }

    return Optional.empty();
  }
}
