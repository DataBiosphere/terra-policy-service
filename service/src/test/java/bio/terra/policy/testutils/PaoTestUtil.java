package bio.terra.policy.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.service.pao.PaoService;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.policy.model.PolicyUpdateResult;
import java.util.Collections;
import java.util.UUID;

public class PaoTestUtil {
  public static final String TERRA_NAMESPACE = "terra";
  public static final String REGION_CONSTRAINT = "region-constraint";
  public static final String GROUP_CONSTRAINT = "group-constraint";
  public static final String DATA_TRACKING_CONSTRAINT = "data-tracking";
  public static final String REGION_KEY = "region-name";
  public static final String REGION_NAME_USA = "usa";
  public static final String REGION_NAME_IOWA = "iowa";
  public static final String REGION_NAME_EUROPE = "europe";
  public static final String GROUP_KEY = "group";
  public static final String GROUP_NAME = "mygroup";
  public static final String GROUP_NAME_ALT = "myaltgroup";
  public static final String DATA_TRACKING_KEY = "dataType";
  public static final String DATA_TYPE_NAME = "PHI";
  public static final String DATA_TYPE_NAME_ALT = "FEDERAL";
  public static final String TEST_NAMESPACE = "test_namespace";
  public static final String TEST_FLAG_POLICY_A = "test_flag_a";
  public static final String TEST_FLAG_POLICY_B = "test_flag_b";
  public static final String TEST_DATA_POLICY_X = "test_data_x";
  public static final String TEST_DATA_POLICY_Y = "test_data_y";
  public static final String DATA_KEY = "key";
  public static final String DATA1 = "data1";
  public static final String DATA2 = "data2";

  public static PolicyInputs makePolicyInputs(PolicyInput... inputList) {
    var inputs = new PolicyInputs();
    for (PolicyInput input : inputList) {
      inputs.addInput(input);
    }
    return inputs;
  }

  public static UUID makePao(PaoService paoService, PolicyInput... inputList) {
    var inputs = makePolicyInputs(inputList);
    UUID id = UUID.randomUUID();
    paoService.createPao(id, PaoComponent.WSM, PaoObjectType.WORKSPACE, inputs);
    return id;
  }

  public static PolicyInput makeFlagInput(String name) {
    return PolicyInput.createFromMap(TEST_NAMESPACE, name, Collections.emptyMap());
  }

  public static PolicyInput makeRegionPolicyInput(String location) {
    return PolicyInput.createFromMap(
        TERRA_NAMESPACE, REGION_CONSTRAINT, Collections.singletonMap(REGION_KEY, location));
  }

  public static PolicyInput makeGroupPolicyInput(String groupName) {
    return PolicyInput.createFromMap(
        TERRA_NAMESPACE, GROUP_CONSTRAINT, Collections.singletonMap(GROUP_KEY, groupName));
  }

  public static PolicyInput makeDataInput(String name, String data) {
    return PolicyInput.createFromMap(
        TEST_NAMESPACE, name, Collections.singletonMap(DATA_KEY, data));
  }

  public static void checkConflictFind(
      PolicyUpdateResult result,
      UUID expectedPaoId,
      UUID expectedConflictId,
      PolicyName expectedPolicyName) {
    PolicyConflict conflict = null;
    for (var tryConflict : result.conflicts()) {
      if (tryConflict.pao().getObjectId().equals(expectedPaoId)) {
        conflict = tryConflict;
      }
    }
    assertNotNull(conflict);
    assertEquals(expectedConflictId, conflict.conflictPao().getObjectId());
    assertEquals(expectedPolicyName, conflict.policyName());
  }

  // Check for when we know what both ids in the conflict will be
  public static void checkConflict(
      PolicyUpdateResult result,
      UUID expectedPaoId,
      UUID expectedConflictId,
      PolicyName expectedPolicyName) {
    checkConflictUnknownId(result, expectedPaoId, expectedPolicyName);
    assertEquals(expectedConflictId, result.conflicts().get(0).conflictPao().getObjectId());
  }

  // Check when we do not know what the conflict id will be
  public static void checkConflictUnknownId(
      PolicyUpdateResult result, UUID expectedPaoId, PolicyName expectedPolicyName) {
    assertEquals(1, result.conflicts().size());
    PolicyConflict conflict = result.conflicts().get(0);
    assertEquals(expectedPaoId, conflict.pao().getObjectId());
    assertEquals(expectedPolicyName, conflict.policyName());
  }

  public static void checkForPolicies(Pao pao, PolicyInput... inputList) {
    PolicyInputs inputs = pao.getEffectiveAttributes();
    for (PolicyInput input : inputList) {
      var foundInput = inputs.lookupPolicy(input);
      assertNotNull(foundInput);
      assertEquals(input.getAdditionalData(), foundInput.getAdditionalData());
    }
  }

  public static void checkForMissingPolicies(Pao pao, PolicyInput... inputList) {
    PolicyInputs inputs = pao.getEffectiveAttributes();
    for (PolicyInput input : inputList) {
      var foundInput = inputs.lookupPolicy(input);
      if (foundInput != null) {
        assertNotEquals(input.getAdditionalData(), foundInput.getAdditionalData());
      }
    }
  }
}
