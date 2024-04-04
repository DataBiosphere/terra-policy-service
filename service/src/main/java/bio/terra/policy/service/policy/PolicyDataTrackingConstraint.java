package bio.terra.policy.service.policy;

import bio.terra.policy.common.model.Constants;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PolicyDataTrackingConstraint extends PolicyBase {
  private static final String DATA_KEY = "dataType";

  @Override
  public PolicyName getPolicyName() {
    return Constants.DATA_TRACKING_POLICY_NAME;
  }

  /**
   * Combine of data types - there is no conflict case as data can fall under multiple categories
   * (e.g. federally protected PHI). We simply create two Sets of data types from the
   * comma-separated form, then mash them together, and make them back into comma-separated form.
   *
   * @param dependent policy input
   * @param source policy input
   * @return policy input
   */
  @Override
  protected PolicyInput performCombine(PolicyInput dependent, PolicyInput source) {
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
    dependentSet.forEach(type -> newData.put(DATA_KEY, type));
    return new PolicyInput(dependent.getPolicyName(), newData);
  }

  /**
   * Remove data tracking
   *
   * @param target existing policy
   * @param removePolicy policy to remove
   * @return the target with dataTypes removed; null if no dataTypes left
   */
  @Override
  protected PolicyInput performRemove(PolicyInput target, PolicyInput removePolicy) {
    Set<String> targetDataTypes = dataToSet(target.getData(DATA_KEY));
    Set<String> removeDataTypes = dataToSet(removePolicy.getData(DATA_KEY));
    targetDataTypes.removeAll(removeDataTypes);

    if (targetDataTypes.isEmpty()) {
      return null;
    }

    Multimap<String, String> newData = ArrayListMultimap.create();
    targetDataTypes.forEach(dataType -> newData.put(DATA_KEY, dataType));
    return new PolicyInput(Constants.DATA_TRACKING_POLICY_NAME, newData);
  }

  /**
   * For dataTypes, the only thing we validate right now is that the key is correct.
   *
   * @param policyInput the input to validate
   * @return
   */
  @Override
  protected boolean performIsValid(PolicyInput policyInput) {
    for (var entry : policyInput.getAdditionalData().entries()) {
      if (!entry.getKey().equals(DATA_KEY)) {
        return false;
      }
    }
    return true;
  }

  @VisibleForTesting
  Set<String> dataToSet(Collection<String> dataTypes) {
    return new HashSet<>(dataTypes);
  }
}
