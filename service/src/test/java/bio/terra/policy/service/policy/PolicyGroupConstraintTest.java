package bio.terra.policy.service.policy;

import static bio.terra.policy.service.policy.PolicyTestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class PolicyGroupConstraintTest extends TestUnitBase {
  @Test
  void groupConstraintTest_combineSameGroups() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    var dependentPolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, GROUP_NAME));
    var sourcePolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, GROUP_NAME));

    PolicyInput resultPolicy = groupConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        groupConstraint.dataToSet(resultPolicy.getAdditionalData().get(GROUP_KEY));

    assertEquals(1, groupSet.size(), "Contains 1 group");
    assertThat(groupSet, contains(GROUP_NAME));
  }

  @Test
  void groupConstraintTest_combineEmptySource() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    var dependentPolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, GROUP_NAME));

    // source policy has no group constraint.
    var sourcePolicy =
        new PolicyInput(TERRA, REGION_CONSTRAINT, buildMultimap(REGION_KEY, GROUP_NAME));

    PolicyInput resultPolicy = groupConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        groupConstraint.dataToSet(resultPolicy.getAdditionalData().get(GROUP_KEY));

    assertEquals(1, groupSet.size(), "Contains 1 group");
    assertThat(groupSet, contains(GROUP_NAME));
  }

  @Test
  void groupConstraintTest_combineEmptySourceAndDestination() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    // Only adding region constraints.
    var dependentPolicy =
        new PolicyInput(TERRA, REGION_CONSTRAINT, buildMultimap(REGION_KEY, GROUP_NAME));
    var sourcePolicy =
        new PolicyInput(TERRA, REGION_CONSTRAINT, buildMultimap(REGION_KEY, GROUP_NAME));

    PolicyInput resultPolicy = groupConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        groupConstraint.dataToSet(resultPolicy.getAdditionalData().get(GROUP_KEY));

    assertEquals(0, groupSet.size());
  }

  @Test
  void groupConstraintTest_combineMismatchGroupsFails() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    var dependentPolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, GROUP_NAME));
    var sourcePolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, GROUP_NAME + "a"));

    PolicyInput resultPolicy = groupConstraint.combine(dependentPolicy, sourcePolicy);
    assertNull(resultPolicy);
  }

  @Test
  void groupConstraintTest_combineMultipleGroups() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    String group2 = GROUP_NAME + "2";
    String group3 = GROUP_NAME + "3";

    Set<String> groups = new HashSet<>(Arrays.asList(GROUP_NAME, group2, group3));

    var dependentPolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));
    var sourcePolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    PolicyInput resultPolicy = groupConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> groupSet =
        groupConstraint.dataToSet(resultPolicy.getAdditionalData().get(GROUP_KEY));

    assertEquals(groups.size(), groupSet.size());
    assertTrue(groupSet.containsAll(groups));
  }

  @Test
  void groupConstraintTest_combineWhenDestinationHasFewerGroups() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    String group2 = GROUP_NAME + "2";
    String group3 = GROUP_NAME + "3";

    Set<String> groups = new HashSet<>(Arrays.asList(GROUP_NAME, group2));

    var dependentPolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    groups.add(group3);
    var sourcePolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    PolicyInput resultPolicy = groupConstraint.combine(dependentPolicy, sourcePolicy);
    assertNull(resultPolicy);
  }

  @Test
  void groupConstraintTest_combineWhenSourceHasFewerGroups() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    String group2 = GROUP_NAME + "2";
    String group3 = GROUP_NAME + "3";

    Set<String> groups = new HashSet<>(Arrays.asList(GROUP_NAME, group2, group3));

    var dependentPolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    groups.remove(group3);
    var sourcePolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    PolicyInput resultPolicy = groupConstraint.combine(dependentPolicy, sourcePolicy);
    assertNull(resultPolicy);
  }

  @Test
  void groupConstraintTest_removeGroup() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    String group2 = GROUP_NAME + "2";
    String group3 = GROUP_NAME + "3";

    Set<String> groups = new HashSet<>(Arrays.asList(GROUP_NAME));
    var removePolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    groups.add(group2);
    groups.add(group3);
    var targetPolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    PolicyInput resultPolicy = groupConstraint.remove(targetPolicy, removePolicy);
    Set<String> groupSet =
        groupConstraint.dataToSet(resultPolicy.getAdditionalData().get(GROUP_KEY));

    assertEquals(2, groupSet.size());
    assertTrue(groupSet.contains(group2));
    assertTrue(groupSet.contains(group3));
  }

  @Test
  void groupConstraintTest_removeNonExistentGroup() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    var removePolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, GROUP_NAME));

    String group2 = GROUP_NAME + "2";
    String group3 = GROUP_NAME + "3";
    Set<String> groups = new HashSet<>(Arrays.asList(group2, group3));
    var targetPolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    // we never added GROUP_NAME into the policy, removing it shouldn't break.
    PolicyInput resultPolicy = groupConstraint.remove(targetPolicy, removePolicy);
    Set<String> groupSet =
        groupConstraint.dataToSet(resultPolicy.getAdditionalData().get(GROUP_KEY));

    assertEquals(2, groupSet.size());
    assertTrue(groupSet.contains(group2));
    assertTrue(groupSet.contains(group3));
  }

  @Test
  void groupConstraintTest_removeOnlyGroup() throws Exception {
    var groupConstraint = new PolicyGroupConstraint();

    Set<String> groups = new HashSet<>(Arrays.asList(GROUP_NAME));
    var removePolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));
    var targetPolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP_KEY, groups));

    PolicyInput resultPolicy = groupConstraint.remove(targetPolicy, removePolicy);
    assertNull(resultPolicy);
  }
}
