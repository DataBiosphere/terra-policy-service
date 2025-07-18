package bio.terra.policy.service.policy;

import bio.terra.policy.common.model.Constants;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.service.region.RegionService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PolicyRegionConstraint extends PolicyBase {
  private static final String DATA_KEY = "region-name";

  private static final RegionService regionService = new RegionService();

  @Override
  public PolicyName getPolicyName() {
    return Constants.REGION_CONSTRAINT_POLICY_NAME;
  }

  /**
   * Combine Regions. There are multiple possibilities here:
   *
   * <pre>
   * 1. There is no overlap between source and dependent, result is a conflict.
   * 2. The regions are equal, in which case either can be selected.
   * 3. One of the regions is a subregion of the other, in which case we select the subregion.
   * 4. The regions are not subregions, but have intersecting data centers. In this case,
   *    we need to make a new set of inputs containing just the intersection. (TODO)
   * </pre>
   */
  @Override
  protected PolicyInput performCombine(PolicyInput dependent, PolicyInput source) {
    if (dependent == null) {
      return source;
    }

    if (source == null) {
      return dependent;
    }

    Set<String> dependentSet = dataToSet(dependent.getData(DATA_KEY));
    Set<String> sourceSet = dataToSet(source.getData(DATA_KEY));
    int sourceSize = sourceSet.size();
    int dependentSize = dependentSet.size();
    Multimap<String, String> newData = ArrayListMultimap.create();

    if (dependentSize == 0 && sourceSize == 0) {
      return null;
    }

    if (dependentSize == 0) {
      newData.putAll(DATA_KEY, sourceSet);
      return new PolicyInput(source.getPolicyName(), newData);
    }

    if (sourceSize == 0) {
      newData.putAll(DATA_KEY, dependentSet);
      return new PolicyInput(dependent.getPolicyName(), newData);
    }

    Set<String> resultSet = new HashSet<>();

    /**
     * n*m algorithm. Compare each source region with each dependent region. If they're the same,
     * then add it to the result set. If one is a subregion of the other, then add the subregion to
     * the result.
     */
    for (String sourceRegion : sourceSet) {
      for (String dependentRegion : dependentSet) {
        if (dependentRegion.equals(sourceRegion)) {
          resultSet.add(sourceRegion);
        } else if (regionService.isSubLocation(dependentRegion, sourceRegion)) {
          resultSet.add(sourceRegion);
        } else if (regionService.isSubLocation(sourceRegion, dependentRegion)) {
          resultSet.add(dependentRegion);
        }
      }
    }

    if (!resultSet.isEmpty()) {
      newData.putAll(DATA_KEY, resultSet);
      return new PolicyInput(Constants.REGION_CONSTRAINT_POLICY_NAME, newData);
    }

    return null;
  }

  /**
   * Remove regions - remove regions in the removePolicy from regions in the target policy Removing
   * a region that is not found is not an error. If there is nothing left over, return null.
   *
   * @param target existing policy
   * @param removePolicy policy to remove
   * @return the target with regions removed; null if no regions left
   */
  @Override
  protected PolicyInput performRemove(PolicyInput target, PolicyInput removePolicy) {
    Set<String> targetRegions = dataToSet(target.getData(DATA_KEY));
    Set<String> removeRegions = dataToSet(removePolicy.getData(DATA_KEY));
    targetRegions.removeAll(removeRegions);

    if (targetRegions.isEmpty()) {
      return null;
    }

    Multimap<String, String> newData = ArrayListMultimap.create();
    targetRegions.forEach(group -> newData.put(DATA_KEY, group));
    return new PolicyInput(Constants.REGION_CONSTRAINT_POLICY_NAME, newData);
  }

  /**
   * Validate that a region policy input has the right key and that the value exists in the
   * ontology.
   *
   * @param policyInput the input to validate
   */
  @Override
  protected boolean performIsValid(PolicyInput policyInput) {
    Multimap<String, String> additionalData = policyInput.getAdditionalData();
    for (String key : additionalData.keySet()) {
      if (!key.equals(DATA_KEY)) {
        return false;
      }

      for (var value : additionalData.get(key)) {
        if (regionService.getLocation(value) == null) {
          return false;
        }
      }
    }
    return true;
  }

  @VisibleForTesting
  Set<String> dataToSet(Collection<String> regions) {
    return new HashSet<>(regions);
  }
}
