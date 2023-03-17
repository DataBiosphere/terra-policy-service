package bio.terra.policy.service.policy;

import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PolicyGroupConstraint implements PolicyBase {
  private static final PolicyName POLICY_NAME = new PolicyName("terra", "group-constraint");
  private static final String DATA_KEY = "group";

  @Override
  public PolicyName getPolicyName() {
    return POLICY_NAME;
  }

  /**
   * Combine of groups - there is no conflict case. We simply create two Sets of group names from
   * the comma-separated form, then mash them together, and make them back into comma-separated
   * form. For milestone 1 limitations, WSM will need to interpret results of the combine and handle
   * enforcement.
   *
   * @param dependent policy input
   * @param source policy input
   * @return policy input
   */
  @Override
  public PolicyInput combine(PolicyInput dependent, PolicyInput source) {
    if (source == null) {
      return dependent;
    }

    if (dependent == null) {
      return source;
    }

    Set<String> dependentSet = dataToSet(dependent.getData(DATA_KEY));
    Set<String> sourceSet = dataToSet(source.getData(DATA_KEY));
    dependentSet.addAll(sourceSet);
    Multimap<String, String> newData = ArrayListMultimap.create();
    dependentSet.forEach(group -> newData.put(DATA_KEY, group));
    return new PolicyInput(dependent.getPolicyName(), newData);
  }

  /**
   * Remove groups - we don't currently have the ability to modify groups, so throw an exception.
   *
   * @param target existing policy
   * @param removePolicy policy to remove
   * @return the target with groups removed; null if no groups left
   */
  @Override
  public PolicyInput remove(PolicyInput target, PolicyInput removePolicy) {
    throw new InvalidInputException("Cannot remove a group constraint");
  }

  /**
   * For groups, the only thing we can validate right now is that the key is correct. TODO PF-2558:
   * When we connect TPS to SAM, we can validate the group name as well.
   *
   * @param policyInput the input to validate
   * @return
   */
  @Override
  public boolean isValid(PolicyInput policyInput) {
    for (var key : policyInput.getAdditionalData().keySet()) {
      if (!key.equals(DATA_KEY)) {
        return false;
      }
    }
    return true;
  }

  @VisibleForTesting
  Set<String> dataToSet(Collection<String> groups) {
    return new HashSet<>(groups);
  }
}
