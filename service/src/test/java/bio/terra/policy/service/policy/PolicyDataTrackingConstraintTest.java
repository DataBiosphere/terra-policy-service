package bio.terra.policy.service.policy;

import static bio.terra.policy.service.policy.PolicyTestUtils.*;
import static bio.terra.policy.testutils.PaoTestUtil.DATA_TRACKING_CONSTRAINT;
import static bio.terra.policy.testutils.PaoTestUtil.DATA_TRACKING_KEY;
import static bio.terra.policy.testutils.PaoTestUtil.DATA_TYPE_NAME;
import static bio.terra.policy.testutils.PaoTestUtil.DATA_TYPE_NAME_ALT;
import static bio.terra.policy.testutils.PaoTestUtil.REGION_KEY;
import static bio.terra.policy.testutils.PaoTestUtil.TERRA_NAMESPACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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

public class PolicyDataTrackingConstraintTest extends TestUnitBase {
  @Test
  void dataTrackingConstraintTest_combineSameDataTypes() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE,
            DATA_TRACKING_CONSTRAINT,
            buildMultimap(DATA_TRACKING_KEY, DATA_TYPE_NAME));
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE,
            DATA_TRACKING_CONSTRAINT,
            buildMultimap(DATA_TRACKING_KEY, DATA_TYPE_NAME));

    PolicyInput resultPolicy = dataTrackingConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(1, dataTypeSet.size(), "Contains 1 dataType");
    assertThat(dataTypeSet, contains(DATA_TYPE_NAME));
  }

  @Test
  void dataTrackingConstraintTest_combineEmptySource() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE,
            DATA_TRACKING_CONSTRAINT,
            buildMultimap(DATA_TRACKING_KEY, DATA_TYPE_NAME));

    PolicyInput resultPolicy = dataTrackingConstraint.combine(dependentPolicy, null);

    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(1, dataTypeSet.size(), "Contains 1 dataType");
    assertThat(dataTypeSet, contains(DATA_TYPE_NAME));
  }

  @Test
  void dataTrackingConstraintTest_combineEmptyDestination() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE,
            DATA_TRACKING_CONSTRAINT,
            buildMultimap(DATA_TRACKING_KEY, DATA_TYPE_NAME));

    PolicyInput resultPolicy = dataTrackingConstraint.combine(null, sourcePolicy);
    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(1, dataTypeSet.size(), "Contains 1 dataType");
    assertThat(dataTypeSet, contains(DATA_TYPE_NAME));
  }

  @Test
  void dataTrackingConstraintTest_combineEmptySourceAndDestination() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    var dependentPolicy =
        new PolicyInput(TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(REGION_KEY));
    var sourcePolicy =
        new PolicyInput(TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(REGION_KEY));

    PolicyInput resultPolicy = dataTrackingConstraint.combine(dependentPolicy, sourcePolicy);

    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(0, dataTypeSet.size());
  }

  @Test
  void dataTrackingConstraintTest_combineMismatchDataTypes() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE,
            DATA_TRACKING_CONSTRAINT,
            buildMultimap(DATA_TRACKING_KEY, DATA_TYPE_NAME));
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE,
            DATA_TRACKING_CONSTRAINT,
            buildMultimap(DATA_TRACKING_KEY, DATA_TYPE_NAME_ALT));

    PolicyInput resultPolicy = dataTrackingConstraint.combine(dependentPolicy, sourcePolicy);
    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(2, dataTypeSet.size());
    assertTrue(dataTypeSet.containsAll(Arrays.asList(DATA_TYPE_NAME, DATA_TYPE_NAME_ALT)));
  }

  @Test
  void dataTrackingConstraintTest_combineMultipleDataTypes() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    String dataType2 = DATA_TYPE_NAME + "2";
    String dataType3 = DATA_TYPE_NAME + "3";

    Set<String> dataTypes = new HashSet<>(Arrays.asList(DATA_TYPE_NAME, dataType2, dataType3));

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    PolicyInput resultPolicy = dataTrackingConstraint.combine(dependentPolicy, sourcePolicy);
    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(dataTypes.size(), dataTypeSet.size());
    assertTrue(dataTypeSet.containsAll(dataTypes));
  }

  @Test
  void dataTrackingConstraintTest_combineWhenDestinationHasFewerDataTypes() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    String dataType2 = DATA_TYPE_NAME + "2";
    String dataType3 = DATA_TYPE_NAME + "3";

    Set<String> dataTypes = new HashSet<>(Arrays.asList(DATA_TYPE_NAME, dataType2));

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    dataTypes.add(dataType3);
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    PolicyInput resultPolicy = dataTrackingConstraint.combine(dependentPolicy, sourcePolicy);
    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(dataTypes.size(), dataTypeSet.size());
    assertTrue(dataTypeSet.containsAll(dataTypes));
  }

  @Test
  void dataTrackingConstraintTest_combineWhenSourceHasFewerDataTypes() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    String dataType2 = DATA_TYPE_NAME + "2";
    String dataType3 = DATA_TYPE_NAME + "3";

    Set<String> dataTypes = new HashSet<>(Arrays.asList(DATA_TYPE_NAME, dataType2, dataType3));

    var dependentPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    dataTypes.remove(dataType3);
    var sourcePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    PolicyInput resultPolicy = dataTrackingConstraint.combine(dependentPolicy, sourcePolicy);
    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(3, dataTypeSet.size());
    assertTrue(dataTypeSet.containsAll(dataTypes));
  }

  @Test
  void dataTrackingConstraintTest_removeDataType() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    String dataType2 = DATA_TYPE_NAME + "2";
    String dataType3 = DATA_TYPE_NAME + "3";

    Set<String> dataTypes = new HashSet<>(Arrays.asList(DATA_TYPE_NAME));
    var removePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    dataTypes.add(dataType2);
    dataTypes.add(dataType3);
    var targetPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    PolicyInput resultPolicy = dataTrackingConstraint.remove(targetPolicy, removePolicy);
    Set<String> dataTypeSet =
        dataTrackingConstraint.dataToSet(resultPolicy.getAdditionalData().get(DATA_TRACKING_KEY));

    assertEquals(2, dataTypeSet.size());
    assertTrue(dataTypeSet.containsAll(Arrays.asList(dataType2, dataType3)));
  }

  @Test
  void dataTrackingConstraintTest_removeAllDataTypes() throws Exception {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    String dataType2 = DATA_TYPE_NAME + "2";
    String dataType3 = DATA_TYPE_NAME + "3";

    Set<String> dataTypes = new HashSet<>(Arrays.asList(DATA_TYPE_NAME, dataType2, dataType3));
    var removePolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    var targetPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    PolicyInput resultPolicy = dataTrackingConstraint.remove(targetPolicy, removePolicy);
    assertNull(resultPolicy);
  }

  @Test
  void dataTrackingConstraintTest_validation() {
    var dataTrackingConstraint = new PolicyDataTrackingConstraint();

    Set<String> dataTypes = new HashSet<>(Arrays.asList(DATA_TYPE_NAME));
    var validPolicy =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));
    var invalidKey =
        new PolicyInput(
            TERRA_NAMESPACE,
            DATA_TRACKING_CONSTRAINT,
            buildMultimap(DATA_TRACKING_KEY + "invalid", dataTypes));

    dataTypes.add("invalid");
    var invalidValue =
        new PolicyInput(
            TERRA_NAMESPACE, DATA_TRACKING_CONSTRAINT, buildMultimap(DATA_TRACKING_KEY, dataTypes));

    assertTrue(dataTrackingConstraint.isValid(validPolicy));
    assertFalse(dataTrackingConstraint.isValid(invalidKey));
    // we don't currently validate the value
    assertTrue(dataTrackingConstraint.isValid(invalidValue));
  }
}
