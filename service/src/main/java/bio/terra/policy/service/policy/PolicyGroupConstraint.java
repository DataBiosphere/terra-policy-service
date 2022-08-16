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
   * Combine of groups - there is no conflict case. We simply create two Sets of group names from
   * the comma-separated form, then mash them together, and make them back into comma-separated
   * form.
   *
   * @param dependent policy input
   * @param source policy input
   * @return policy input
   */
  @Override
  public PolicyInput combine(PolicyInput dependent, PolicyInput source) {
    Set<String> dependentSet = dataToSet(dependent.getData(DATA_KEY));
    Set<String> sourceSet = dataToSet(source.getData(DATA_KEY));
    dependentSet.addAll(sourceSet);
    Multimap<String, String> newData = ArrayListMultimap.create();
    dependentSet.forEach(group -> newData.put(DATA_KEY, group));
    return new PolicyInput(dependent.getPolicyName(), newData);
  }

  @VisibleForTesting
  Set<String> dataToSet(Collection<String> groups) {
    return new HashSet<>(groups);
  }
}
