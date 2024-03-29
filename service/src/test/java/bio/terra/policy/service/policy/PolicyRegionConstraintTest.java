package bio.terra.policy.service.policy;

import static bio.terra.policy.service.policy.PolicyTestUtils.buildMultimap;
import static bio.terra.policy.testutils.PaoTestUtil.GROUP_KEY;
import static bio.terra.policy.testutils.PaoTestUtil.REGION_CONSTRAINT;
import static bio.terra.policy.testutils.PaoTestUtil.REGION_KEY;
import static bio.terra.policy.testutils.PaoTestUtil.TERRA_NAMESPACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class PolicyRegionConstraintTest extends TestUnitBase {

  @Test
  void regionConstraintTest_combineSameRegion() throws Exception {
    var regionConstraint = new PolicyRegionConstraint();

    var region1 = "usa";
    var region2 = region1;

    var dependentPolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, region1));
    var sourcePolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, region2));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(1, groupSet.size(), "Contains 1 region");
    assertThat(groupSet, contains(region1));
  }

  @Test
  void regionConstraintTest_combineSubRegion() throws Exception {
    var regionConstraint = new PolicyRegionConstraint();

    var region1 = "europe";
    var region2 = "germany";

    var dependentPolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, region1));
    var sourcePolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, region2));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(1, groupSet.size(), "Contains 1 region");
    assertThat(groupSet, contains(region2));
  }

  @Test
  void regionConstraintTest_combineEmptyDependent() {
    var regionConstraint = new PolicyRegionConstraint();

    var sourceRegion = "germany";

    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, sourceRegion));

    PolicyInput resultPolicy = regionConstraint.combine(null, sourcePolicy);

    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(1, groupSet.size(), "Contains 1 region");
    assertThat(groupSet, contains(sourceRegion));
  }

  @Test
  void regionConstraintTest_combineEmptySource() {
    var regionConstraint = new PolicyRegionConstraint();

    var dependentRegion = "germany";

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, dependentRegion));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, null);

    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(1, groupSet.size(), "Contains 1 region");
    assertThat(groupSet, contains(dependentRegion));
  }

  @Test
  void regionConstraintTest_combineEmptySets() {
    var regionConstraint = new PolicyRegionConstraint();

    var dependentPolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(GROUP_KEY));
    var sourcePolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(GROUP_KEY));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, sourcePolicy);

    assertNull(resultPolicy);
  }

  @Test
  // The source regions are subregions of the dependents. Result should be the smaller of the 2.
  void regionConstraintTest_combineReducesToSources() {
    var regionConstraint = new PolicyRegionConstraint();

    Set<String> dependentRegions =
        new HashSet<>(Arrays.asList("americas", "asiapacific", "europe"));
    Set<String> sourceRegions = new HashSet<>(Arrays.asList("uk", "gcp.us-central1", "japan"));

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, dependentRegions));
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, sourceRegions));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(3, groupSet.size(), "Contains 3 regions");
    assertThat(groupSet, containsInAnyOrder(sourceRegions.toArray()));
  }

  @Test
  // The dependent regions are subregions of the sources. Result should be the smaller of the 2.
  void regionConstraintTest_combineReducesToDependents() {
    var regionConstraint = new PolicyRegionConstraint();

    Set<String> dependentRegions = new HashSet<>(Arrays.asList("uk", "gcp.us-central1", "japan"));
    Set<String> sourceRegions = new HashSet<>(Arrays.asList("americas", "asiapacific", "europe"));

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, dependentRegions));
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, sourceRegions));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(3, groupSet.size(), "Contains 3 regions");
    assertThat(groupSet, containsInAnyOrder(dependentRegions.toArray()));
  }

  @Test
  // Combine should throw out source regions if they are not equal to or a subregion of a dependent.
  void regionConstraintTest_combineMultipleRegionsExcludeSource() {
    var regionConstraint = new PolicyRegionConstraint();

    Set<String> dependentRegions = new HashSet<>(Arrays.asList("europe"));
    Set<String> sourceRegions = new HashSet<>(Arrays.asList("usa", "asiapacific", "uk", "finland"));
    Set<String> expectedResult = new HashSet<>(Arrays.asList("uk", "finland"));

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, dependentRegions));
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, sourceRegions));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(2, groupSet.size(), "Contains 2 regions");
    assertThat(groupSet, containsInAnyOrder(expectedResult.toArray()));
  }

  @Test
  // Combine should throw out dependent regions if they are not equal to or a subregion of a source.
  void regionConstraintTest_combineMultipleRegionsExcludeDependent() {
    var regionConstraint = new PolicyRegionConstraint();

    Set<String> dependentRegions =
        new HashSet<>(Arrays.asList("usa", "asiapacific", "uk", "finland"));
    Set<String> sourceRegions = new HashSet<>(Arrays.asList("europe"));
    Set<String> expectedResult = new HashSet<>(Arrays.asList("uk", "finland"));

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, dependentRegions));
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, sourceRegions));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(2, groupSet.size(), "Contains 2 regions");
    assertThat(groupSet, containsInAnyOrder(expectedResult.toArray()));
  }

  @Test
  // If there's no overlap of regions, then the result should be null.
  void regionConstraintTest_combineMultipleRegionsWithNoIntersection() {
    var regionConstraint = new PolicyRegionConstraint();

    Set<String> dependentRegions = new HashSet<>(Arrays.asList("usa", "asiapacific"));
    Set<String> sourceRegions = new HashSet<>(Arrays.asList("europe"));

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, dependentRegions));
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, sourceRegions));

    PolicyInput resultPolicy = regionConstraint.combine(dependentPolicy, sourcePolicy);

    assertNull(resultPolicy);
  }

  @Test
  void regionConstraintTest_removeRegion() {
    var regionConstraint = new PolicyRegionConstraint();

    var region1 = "europe";
    var region2 = "usa";

    var targetPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, region1, region2));
    var removePolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, region2));

    PolicyInput resultPolicy = regionConstraint.remove(targetPolicy, removePolicy);
    Set<String> groupSet =
        regionConstraint.dataToSet(resultPolicy.getAdditionalData().get(REGION_KEY));
    assertEquals(1, groupSet.size(), "Contains 1 region");
    assertThat(groupSet, contains(region1));
  }

  @Test
  void regionConstraintTest_removeAllRegions() {
    var regionConstraint = new PolicyRegionConstraint();

    var region1 = "europe";

    var targetPolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, region1));
    var removePolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, region1));

    PolicyInput resultPolicy = regionConstraint.remove(targetPolicy, removePolicy);
    assertNull(resultPolicy);
  }

  @Test
  void regionConstraintTest_validation() {
    var regionConstraint = new PolicyRegionConstraint();

    var location1 = "europe";

    var validPolicy =
        new PolicyInput(TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, location1));
    var validMultiValue =
        new PolicyInput(
            TERRA_NAMESPACE,
            REGION_CONSTRAINT,
            buildMultimap(REGION_KEY, location1, "usa", "asiapacific"));
    var invalidKey =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY + "invalid", location1));
    var invalidValue =
        new PolicyInput(
            TERRA_NAMESPACE, REGION_CONSTRAINT, buildMultimap(REGION_KEY, location1 + "invalid"));

    assertTrue(regionConstraint.isValid(validPolicy));
    assertTrue(regionConstraint.isValid(validMultiValue));
    assertFalse(regionConstraint.isValid(invalidKey));
    assertFalse(regionConstraint.isValid(invalidValue));
  }
}
