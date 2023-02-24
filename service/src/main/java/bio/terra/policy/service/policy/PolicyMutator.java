package bio.terra.policy.service.policy;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInput;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;

/**
 * Static methods for mutating policies. In general, these methods locate the correct policy object
 * for doing the mutation and then call the method on that object. Known objects are found in
 * KnownPolicy enumeration. Unknown policies use the PolicyUnknown. Since mutators are specific to a
 * policy name, we keep the unknown mutators in a static map and reuse them.
 */
public class PolicyMutator {
  private static final Map<String, PolicyBase> unknownPolicyMap = new ConcurrentHashMap<>();

  /**
   * Combine two policy inputs. Return null if the inputs are in conflict.
   *
   * @param target the target input - it is taking (additional) policy state from the addPolicy
   * @param addPolicy the source input - it is providing policy state
   * @return A new PolicyInput object reflecting
   */
  public static PolicyInput combine(PolicyInput target, PolicyInput addPolicy) {
    validateMatchedPolicies(target, addPolicy);
    return findPolicy(target == null ? addPolicy : target).combine(target, addPolicy);
  }

  public static PolicyInput remove(PolicyInput target, PolicyInput removePolicy) {
    validateMatchedPolicies(target, removePolicy);
    return findPolicy(target == null ? removePolicy : target).remove(target, removePolicy);
  }

  private static void validateMatchedPolicies(PolicyInput one, PolicyInput two) {
    if (one == null || two == null) {
      // We can still call combine if one of the policies is empty.
      return;
    }
    // Ensure that the inputs are combine-able; this shouldn't happen, but... belts and suspenders.
    if (!StringUtils.equals(one.getKey(), two.getKey())) {
      throw new InternalTpsErrorException("Policies have different policy keys");
    }
  }

  private static PolicyBase findPolicy(PolicyInput policyInput) {
    Optional<PolicyBase> policyOptional =
        KnownPolicy.findPolicyBaseByName(policyInput.getPolicyName());
    return policyOptional.orElse(findOrCreateUnknownPolicy(policyInput));
  }

  private static PolicyBase findOrCreateUnknownPolicy(PolicyInput dependent) {
    PolicyBase policy = unknownPolicyMap.get(dependent.getPolicyNameKey());
    if (policy == null) {
      policy = new PolicyUnknown(dependent.getPolicyName());
      unknownPolicyMap.put(dependent.getKey(), policy);
    }
    return policy;
  }
}
