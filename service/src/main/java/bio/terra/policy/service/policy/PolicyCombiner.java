package bio.terra.policy.service.policy;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.exception.PolicyNotImplementedException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public enum PolicyCombiner {
  GROUP_CONSTRAINT(new PolicyGroupConstraint());

  private final PolicyBase policy;

  PolicyCombiner(PolicyBase policy) {
    this.policy = policy;
  }

  public PolicyBase getPolicy() {
    return policy;
  }

  public static PolicyInput combine(PolicyInput dependent, PolicyInput source) {
    // Ensure that the inputs are combine-able; this shouldn't happen, but... belts and suspenders.
    if (!StringUtils.equals(dependent.getKey(), source.getKey())) {
      throw new InternalTpsErrorException("Dependent and source have different policy keys");
    }
    Optional<PolicyBase> optionalPolicy = findPolicyBaseByName(dependent.getPolicyName());
    if (optionalPolicy.isEmpty()) {
      throw new PolicyNotImplementedException("Combining unknown policies is not yet implemented");
    }
    return optionalPolicy.get().combine(dependent, source);
  }

  public static Optional<PolicyBase> findPolicyBaseByName(PolicyName name) {
    for (PolicyCombiner policyCombiner : values()) {
      if (policyCombiner.getPolicy().getPolicyName().equals(name)) {
        return Optional.of(policyCombiner.getPolicy());
      }
    }
    return Optional.empty();
  }
}
