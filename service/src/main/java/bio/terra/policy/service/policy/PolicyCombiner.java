package bio.terra.policy.service.policy;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInput;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;

/**
 * The policy combiner is a collection of static methods that finds or constructs an object of type
 * PolicyBase for combining policies.
 *
 * <p>Eventually, I expect to have general-purpose combiner algorithms specified in the database
 * description of policies, so I build the unknownPolicyMap to keep around the combiner so we don't
 * repeatedly read the database.
 */
public class PolicyCombiner {
  private static final Map<String, PolicyBase> unknownPolicyMap = new ConcurrentHashMap<>();

  /**
   * Combine two policy inputs. Return null if the inputs are in conflict.
   *
   * @param dependent the dependent input - it is taking (additional) policy state from the source
   * @param source the source input - it is providing policy state
   * @return A new PolicyInput object reflecting
   */
  public static PolicyInput combine(PolicyInput dependent, PolicyInput source) {
    // Ensure that the inputs are combine-able; this shouldn't happen, but... belts and suspenders.
    if (!StringUtils.equals(dependent.getKey(), source.getKey())) {
      throw new InternalTpsErrorException("Dependent and source have different policy keys");
    }

    Optional<PolicyBase> policyOptional =
        PolicyKnownPolicy.findPolicyBaseByName(dependent.getPolicyName());

    PolicyBase policy = policyOptional.orElse(findOrCreateUnknownPolicy(dependent));
    return policy.combine(dependent, source);
  }

  private static PolicyBase findOrCreateUnknownPolicy(PolicyInput dependent) {
    PolicyBase policy = unknownPolicyMap.get(dependent.getPolicyNameKey());
    if (policy == null) {
      policy = new PolicyUnknownPolicyCombiner(dependent.getPolicyName());
      unknownPolicyMap.put(dependent.getKey(), policy);
    }
    return policy;
  }
}
