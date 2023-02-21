package bio.terra.policy.service.policy;

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
   * Combine for groups - for our milestone 1, we can only merge if the source has no groups or the
   * source's groups match exactly the dependent groups. In either case, the result is equal to the
   * dependent. In any other case, the result is a conflict.
   *
   * @param dependent policy input
   * @param source policy input
   * @return policy input
   */
  @Override
  public PolicyInput combine(PolicyInput dependent, PolicyInput source) {
    Set<String> dependentSet = dataToSet(dependent.getData(DATA_KEY));
    Set<String> sourceSet = dataToSet(source.getData(DATA_KEY));

    if (sourceSet.size() == 0
        || (sourceSet.containsAll(dependentSet) && dependentSet.containsAll(sourceSet))) {
      return dependent;
    }

    return null;
  }

  /**
   * Remove groups - remove groups in the removePolicy from groups in the target policy Removing a
   * group that is not found is not an error. If there is nothing left over, return null.
   *
   * @param target existing policy
   * @param removePolicy policy to remove
   * @return the target with groups removed; null if no groups left
   */
  @Override
  public PolicyInput remove(PolicyInput target, PolicyInput removePolicy) {
    Set<String> targetGroups = dataToSet(target.getData(DATA_KEY));
    Set<String> removeGroups = dataToSet(removePolicy.getData(DATA_KEY));
    targetGroups.removeAll(removeGroups);
    if (targetGroups.isEmpty()) {
      return null;
    }

    Multimap<String, String> newData = ArrayListMultimap.create();
    targetGroups.forEach(group -> newData.put(DATA_KEY, group));
    return new PolicyInput(target.getPolicyName(), newData);
  }

  @VisibleForTesting
  Set<String> dataToSet(Collection<String> groups) {
    return new HashSet<>(groups);
  }
}
