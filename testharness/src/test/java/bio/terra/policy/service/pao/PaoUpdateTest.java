package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.exception.DirectConflictException;
import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.service.policy.PolicyMutator;
import bio.terra.policy.service.policy.model.PolicyUpdateResult;
import bio.terra.policy.testutils.LibraryTestBase;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PaoUpdateTest extends LibraryTestBase {
  private static final Logger logger = LoggerFactory.getLogger(PaoUpdateTest.class);
  private static final String TEST_NAMESPACE = "test_namespace";
  private static final String TEST_FLAG_POLICY_A = "test_flag_a";
  private static final String TEST_FLAG_POLICY_B = "test_flag_b";
  private static final String TEST_DATA_POLICY_X = "test_data_x";
  private static final String TEST_DATA_POLICY_Y = "test_data_y";
  private static final String DATA_KEY = "key";
  private static final String DATA1 = "data1";
  private static final String DATA2 = "data2";
  private static final String DATA3 = "data3";

  @Autowired private PaoService paoService;

  @Test
  void unknownFlagCombinerTest() throws Exception {
    // Flag policies refer to unknown policies that have no additional data.
    // They act like flags. If anyone sets the flag, it is set.

    var flagPolicyA1 = makeFlagInput(TEST_FLAG_POLICY_A);
    var flagPolicyA2 = makeFlagInput(TEST_FLAG_POLICY_A);

    // Combining two of the same flag should result in the A flag being set.
    PolicyInput flagResult = PolicyMutator.combine(flagPolicyA1, flagPolicyA2);
    assertEquals(flagResult.getKey(), flagPolicyA1.getKey());
    assertEquals(0, flagResult.getAdditionalData().size());

    // It is an error to combine policies with different names. This is an internal
    // error, because in the production code, combiner check should never happen.
    var flagPolicyB = makeFlagInput(TEST_FLAG_POLICY_B);
    assertThrows(
        InternalTpsErrorException.class, () -> PolicyMutator.combine(flagPolicyA1, flagPolicyB));
  }

  @Test
  void unknownDataCombinerTest() throws Exception {
    // The term "data policy" here refers to an unknown policy with additional data.
    // The rule in the PolicyUnknown implementation is that if the data is different
    // the policies conflict.
    var data1Policy1 = makeDataInput(TEST_DATA_POLICY_X, DATA1);
    var data1Policy2 = makeDataInput(TEST_DATA_POLICY_X, DATA1);

    // Combining identical data policies should result in a single data policy with the same
    // additional data
    PolicyInput dataResult = PolicyMutator.combine(data1Policy1, data1Policy2);
    assertEquals(dataResult.getKey(), data1Policy1.getKey());
    assertEquals(dataResult.getAdditionalData(), data1Policy1.getAdditionalData());

    // Combining the same policy name with different additional data should be a conflict.
    // The combiner reports conflicts by returning null.
    var data2Policy = makeDataInput(TEST_DATA_POLICY_X, DATA2);
    dataResult = PolicyMutator.combine(data1Policy1, data2Policy);
    assertNull(dataResult);
  }

  @Test
  void linkSourceToEmptyTest_policiesPropagateCorrectly() throws Exception {
    // PaoA - has flag A and data policy X, data1
    UUID paoAid =
        makePao(makeFlagInput(TEST_FLAG_POLICY_A), makeDataInput(TEST_DATA_POLICY_X, DATA1));
    logger.info("paoAid: {}", paoAid);

    // PaoB - has flag B and data policy X, data1
    UUID paoBid =
        makePao(makeFlagInput(TEST_FLAG_POLICY_B), makeDataInput(TEST_DATA_POLICY_X, DATA1));
    logger.info("paoBid: {}", paoBid);

    // PaoC - has data policy Y, data 1
    UUID paoCid = makePao(makeDataInput(TEST_DATA_POLICY_Y, DATA1));
    logger.info("paoCid: {}", paoCid);

    // PaoD - has data policy Y, data2
    UUID paoDid = makePao(makeDataInput(TEST_DATA_POLICY_Y, DATA2));
    logger.info("paoDid: {}", paoDid);

    // PaoNone - has no policies
    UUID paoNone = makePao();
    logger.info("paoNone: {}", paoNone);

    // Link A to None - none should get flag A and data policy X, data1
    PolicyUpdateResult result =
        paoService.linkSourcePao(paoNone, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link A to None result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    checkForPolicies(
        resultPao, makeFlagInput(TEST_FLAG_POLICY_A), makeDataInput(TEST_DATA_POLICY_X, DATA1));

    // Link B to None - none should get flag A, flag B, data policy X, data1
    result = paoService.linkSourcePao(paoNone, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link B to None result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoBid));
    checkForPolicies(
        resultPao,
        makeFlagInput(TEST_FLAG_POLICY_A),
        makeFlagInput(TEST_FLAG_POLICY_B),
        makeDataInput(TEST_DATA_POLICY_X, DATA1));

    // Link C to None - none should get flag A, flag B, data policy X, data1
    // and data policy Y data 1
    result = paoService.linkSourcePao(paoNone, paoCid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link C to None result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoBid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    checkForPolicies(
        resultPao,
        makeFlagInput(TEST_FLAG_POLICY_A),
        makeFlagInput(TEST_FLAG_POLICY_B),
        makeDataInput(TEST_DATA_POLICY_X, DATA1),
        makeDataInput(TEST_DATA_POLICY_Y, DATA1));

    // Link D to None - none should stay in previous state with data policy Y with a conflict
    result = paoService.linkSourcePao(paoNone, paoDid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link D to None result: {}", result);
    checkConflict(result, paoNone, paoDid, new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_Y));

    // Same comparison as with Link C to None
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoBid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    checkForPolicies(
        resultPao,
        makeFlagInput(TEST_FLAG_POLICY_A),
        makeFlagInput(TEST_FLAG_POLICY_B),
        makeDataInput(TEST_DATA_POLICY_X, DATA1),
        makeDataInput(TEST_DATA_POLICY_Y, DATA1));

    // Link C to D - D should stay the same with data policy Y in conflict
    result = paoService.linkSourcePao(paoDid, paoCid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link C to D result: {}", result);
    checkConflict(result, paoDid, paoCid, new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_Y));

    // In this case, the resultPao is showing the failed state: with the linked source in place.
    // NOTE: Is that the right thing? I think so: it provides the details of policies that would be
    // included in the dependent that did not conflict.
    // The alternative would be to return the initialPao if the operation fails.
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    checkForPolicies(resultPao, makeDataInput(TEST_DATA_POLICY_Y, DATA2));

    // While we are here, make sure the invalid input case is caught.
    assertThrows(
        InternalTpsErrorException.class,
        () -> paoService.linkSourcePao(paoDid, paoCid, PaoUpdateMode.ENFORCE_CONFLICTS));
  }

  @Test
  void linkSourceHierarchyTest_policesCombineCorrectly() throws Exception {
    // PaoA - has flag A and data policy X, data1
    UUID paoAid =
        makePao(makeFlagInput(TEST_FLAG_POLICY_A), makeDataInput(TEST_DATA_POLICY_X, DATA1));
    logger.info("paoAid: {}", paoAid);

    // PaoB - has flag B and data policy X, data1
    UUID paoBid =
        makePao(makeFlagInput(TEST_FLAG_POLICY_B), makeDataInput(TEST_DATA_POLICY_X, DATA1));
    logger.info("paoBid: {}", paoBid);

    // PaoC - has data policy Y, data 1
    UUID paoCid = makePao(makeDataInput(TEST_DATA_POLICY_Y, DATA1));
    logger.info("paoCid: {}", paoCid);

    // PaoD - has data policy Y, data2
    UUID paoDid = makePao(makeDataInput(TEST_DATA_POLICY_Y, DATA2));
    logger.info("paoDid: {}", paoDid);

    // PaoNone - has no policies
    UUID paoNone = makePao();
    logger.info("paoNone: {}", paoNone);

    // Link A to B
    PolicyUpdateResult result =
        paoService.linkSourcePao(paoBid, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link A to B result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    checkForPolicies(
        resultPao,
        makeFlagInput(TEST_FLAG_POLICY_A),
        makeFlagInput(TEST_FLAG_POLICY_B),
        makeDataInput(TEST_DATA_POLICY_X, DATA1));

    // Link A to C
    result = paoService.linkSourcePao(paoCid, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link A to C result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    checkForPolicies(
        resultPao,
        makeFlagInput(TEST_FLAG_POLICY_A),
        makeDataInput(TEST_DATA_POLICY_X, DATA1),
        makeDataInput(TEST_DATA_POLICY_Y, DATA1));

    // Link B to C
    result = paoService.linkSourcePao(paoCid, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link B to C result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoBid));
    checkForPolicies(
        resultPao,
        makeFlagInput(TEST_FLAG_POLICY_A),
        makeFlagInput(TEST_FLAG_POLICY_B),
        makeDataInput(TEST_DATA_POLICY_X, DATA1),
        makeDataInput(TEST_DATA_POLICY_Y, DATA1));

    // Link C to None
    // and data policy Y data 1
    result = paoService.linkSourcePao(paoNone, paoCid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link C to None result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    checkForPolicies(
        resultPao,
        makeFlagInput(TEST_FLAG_POLICY_A),
        makeFlagInput(TEST_FLAG_POLICY_B),
        makeDataInput(TEST_DATA_POLICY_X, DATA1),
        makeDataInput(TEST_DATA_POLICY_Y, DATA1));

    // Link D to None - none should stay in previous state with data policy Y with a conflict
    result = paoService.linkSourcePao(paoNone, paoDid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link D to None result: {}", result);
    // Check that we got the right conflict
    assertEquals(1, result.conflicts().size());
    PolicyConflict conflict = result.conflicts().get(0);
    assertEquals(paoNone, conflict.pao().getObjectId());
    assertEquals(paoDid, conflict.conflictPao().getObjectId());
    assertEquals(new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_Y), conflict.policyName());
    // Check that None remains unchanged
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    checkForPolicies(
        resultPao,
        makeFlagInput(TEST_FLAG_POLICY_A),
        makeFlagInput(TEST_FLAG_POLICY_B),
        makeDataInput(TEST_DATA_POLICY_X, DATA1),
        makeDataInput(TEST_DATA_POLICY_Y, DATA1));
  }

  @Test
  void updateSinglePaoTest_paoAttributesUpdateCorrectly() throws Exception {
    // Basic updates on a single Pao
    // Pao has flag A and data policy X, data1
    UUID paoId =
        makePao(makeFlagInput(TEST_FLAG_POLICY_A), makeDataInput(TEST_DATA_POLICY_X, DATA1));
    logger.info("paoId: {}", paoId);

    PolicyInputs removes = makePolicyInputs(makeDataInput(TEST_DATA_POLICY_X, DATA1));
    PolicyInputs adds =
        makePolicyInputs(makeFlagInput(TEST_FLAG_POLICY_B), makeFlagInput(TEST_FLAG_POLICY_A));

    PolicyUpdateResult result =
        paoService.updatePao(paoId, adds, removes, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 1 removes {} adds {} result {}", removes, adds, result);
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    checkForPolicies(
        resultPao, makeFlagInput(TEST_FLAG_POLICY_A), makeFlagInput(TEST_FLAG_POLICY_B));
    checkForMissingPolicies(resultPao, makeDataInput(TEST_DATA_POLICY_X, DATA1));

    // Repeating the operation should get the same result.
    result = paoService.updatePao(paoId, adds, removes, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 2 removes {} adds {} result {}", removes, adds, result);
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    checkForPolicies(
        resultPao, makeFlagInput(TEST_FLAG_POLICY_A), makeFlagInput(TEST_FLAG_POLICY_B));
    checkForMissingPolicies(resultPao, makeDataInput(TEST_DATA_POLICY_X, DATA1));

    // Go from zero to one policy
    UUID paoNone = makePao();
    logger.info("paoNone: {}", paoNone);
    PolicyInputs empty = makePolicyInputs();
    PolicyInputs oneFlag = makePolicyInputs(makeFlagInput(TEST_FLAG_POLICY_A));
    result = paoService.updatePao(paoNone, oneFlag, empty, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 3 removes {} adds {} result {}", removes, adds, result);
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    checkForPolicies(resultPao, makeFlagInput(TEST_FLAG_POLICY_A));

    // Go from one to zero policy
    result = paoService.updatePao(paoNone, empty, oneFlag, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 4 removes {} adds {} result {}", removes, adds, result);
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getEffectiveAttributes().getInputs().isEmpty());

    // Try to add a conflicting policy
    UUID paoConflict = makePao(makeDataInput(TEST_DATA_POLICY_X, DATA1));
    logger.info("paoConflict: {}", paoConflict);
    PolicyInputs conflictPolicy = makePolicyInputs(makeDataInput(TEST_DATA_POLICY_X, DATA2));

    assertThrows(
        DirectConflictException.class,
        () ->
            paoService.updatePao(
                paoConflict, conflictPolicy, empty, PaoUpdateMode.ENFORCE_CONFLICTS));
  }

  @Test
  void updateOneSourceTest_sourcePolicyPropagatesCorrectly() throws Exception {
    // Start empty S --> D
    // add to S; should show up in D
    // remove from S; should disappear from D

    UUID paoSid = makePao();
    logger.info("paoSid: {}", paoSid);
    UUID paoDid = makePao();
    logger.info("paoDid: {}", paoDid);
    paoService.linkSourcePao(paoDid, paoSid, PaoUpdateMode.FAIL_ON_CONFLICT);

    PolicyInputs empty = makePolicyInputs();
    PolicyInputs oneFlag = makePolicyInputs(makeFlagInput(TEST_FLAG_POLICY_A));
    // Add the flag policy
    PolicyUpdateResult result =
        paoService.updatePao(paoSid, oneFlag, empty, PaoUpdateMode.FAIL_ON_CONFLICT);
    assertTrue(result.conflicts().isEmpty());
    checkForPolicies(result.computedPao(), makeFlagInput(TEST_FLAG_POLICY_A));

    // Double check that the policy got saved properly
    Pao checkD = paoService.getPao(paoDid);
    checkForPolicies(checkD, makeFlagInput(TEST_FLAG_POLICY_A));

    // Remove the flag policy
    result = paoService.updatePao(paoSid, empty, oneFlag, PaoUpdateMode.FAIL_ON_CONFLICT);
    assertTrue(result.conflicts().isEmpty());
    assertTrue(result.computedPao().getEffectiveAttributes().getInputs().isEmpty());

    // Double check that the policy got saved properly
    checkD = paoService.getPao(paoDid);
    assertTrue(checkD.getEffectiveAttributes().getInputs().isEmpty());
  }

  @Test
  void updatePropagateConflictTest_sourcesConflict() throws Exception {
    // Two sources and a dependent
    // Start S1 empty, S2 policy X data2; link both to D
    // add S1 policy X data1; conflict - try with DRY_RUN, FAIL_ON_CONFLICT, ENFORCE_CONFLICT
    PolicyInput xData2 = makeDataInput(TEST_DATA_POLICY_X, DATA2);

    UUID paoS1id = makePao();
    logger.info("paoS1id: {}", paoS1id);
    UUID paoS2id = makePao(xData2);
    logger.info("paoS2id: {}", paoS2id);
    UUID paoDid = makePao();
    logger.info("paoDid: {}", paoDid);
    paoService.linkSourcePao(paoDid, paoS1id, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(paoDid, paoS2id, PaoUpdateMode.FAIL_ON_CONFLICT);

    // Check that the dependent has the right policy
    Pao checkD = paoService.getPao(paoDid);
    checkForPolicies(checkD, xData2);

    // Conflict case - DRY_RUN
    PolicyInputs empty = makePolicyInputs();
    PolicyInputs conflictPolicy = makePolicyInputs(makeDataInput(TEST_DATA_POLICY_X, DATA1));

    PolicyUpdateResult result =
        paoService.updatePao(paoS1id, conflictPolicy, empty, PaoUpdateMode.DRY_RUN);
    checkConflict(result, paoDid, paoS1id, new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_X));
    checkD = paoService.getPao(paoDid);
    checkForPolicies(checkD, xData2);
    PolicyInput conflicted = checkD.getEffectiveAttributes().getInputs().get(xData2.getKey());
    assertEquals(0, conflicted.getConflicts().size());

    // Conflict case - FAIL_ON_CONFLICT
    // should have the same result as DRY_RUN, since there is a conflict
    result = paoService.updatePao(paoS1id, conflictPolicy, empty, PaoUpdateMode.FAIL_ON_CONFLICT);
    checkConflict(result, paoDid, paoS1id, new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_X));
    checkD = paoService.getPao(paoDid);
    checkForPolicies(checkD, xData2);
    conflicted = checkD.getEffectiveAttributes().getInputs().get(xData2.getKey());
    assertEquals(0, conflicted.getConflicts().size());

    // Conflict case - ENFORCE_CONFLICTS
    result = paoService.updatePao(paoS1id, conflictPolicy, empty, PaoUpdateMode.ENFORCE_CONFLICTS);
    checkConflict(result, paoDid, paoS1id, new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_X));
    checkD = paoService.getPao(paoDid);
    checkForPolicies(checkD, xData2);

    conflicted = checkD.getEffectiveAttributes().getInputs().get(xData2.getKey());
    assertEquals(1, conflicted.getConflicts().size());
    assertTrue(conflicted.getConflicts().contains(paoS1id));
  }

  @Test
  void updatePropagateConflictTest_dependentsConflict() throws Exception {
    // We build this graph A --> B(X-data1) --> C --> D
    // Then we attempt to update A(X-data2) for DRY_RUN, FAIL_ON_CONFLICT, and ENFORCE_CONFLICTS
    // We expect the conflict to propagate from B to C and D.
    PolicyInput xData1 = makeDataInput(TEST_DATA_POLICY_X, DATA1);
    PolicyInput xData2 = makeDataInput(TEST_DATA_POLICY_X, DATA2);
    PolicyInputs conflictPolicy = makePolicyInputs(xData2);
    PolicyInputs emptyPolicy = makePolicyInputs();

    UUID paoAid = makePao();
    UUID paoBid = makePao(xData1);
    UUID paoCid = makePao();
    UUID paoDid = makePao();

    // Hook up the graph and test the policies end up where we expect them
    paoService.linkSourcePao(paoBid, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(paoDid, paoCid, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(paoCid, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    checkForPolicies(paoService.getPao(paoDid), xData1);
    checkForPolicies(paoService.getPao(paoCid), xData1);
    checkForPolicies(paoService.getPao(paoBid), xData1);
    checkForMissingPolicies(paoService.getPao(paoAid), xData1);

    PolicyUpdateResult result =
        paoService.updatePao(paoAid, conflictPolicy, emptyPolicy, PaoUpdateMode.DRY_RUN);
    logger.info("Result: {}", result);
    assertEquals(3, result.conflicts().size()); // B, C, and D
    checkConflictFind(result, paoBid, paoAid, xData1.getPolicyName());
    checkConflictFind(result, paoCid, paoBid, xData1.getPolicyName());
    checkConflictFind(result, paoDid, paoCid, xData1.getPolicyName());

    result =
        paoService.updatePao(paoAid, conflictPolicy, emptyPolicy, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Result: {}", result);
    assertEquals(3, result.conflicts().size()); // B, C, and D
    checkConflictFind(result, paoBid, paoAid, xData1.getPolicyName());
    checkConflictFind(result, paoCid, paoBid, xData1.getPolicyName());
    checkConflictFind(result, paoDid, paoCid, xData1.getPolicyName());

    result =
        paoService.updatePao(paoAid, conflictPolicy, emptyPolicy, PaoUpdateMode.ENFORCE_CONFLICTS);
    logger.info("Result: {}", result);
    assertEquals(3, result.conflicts().size()); // B, C, and D
    checkConflictFind(result, paoBid, paoAid, xData1.getPolicyName());
    checkConflictFind(result, paoCid, paoBid, xData1.getPolicyName());
    checkConflictFind(result, paoDid, paoCid, xData1.getPolicyName());
  }

  private void checkConflictFind(
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

  private void checkConflict(
      PolicyUpdateResult result,
      UUID expectedPaoId,
      UUID expectedConflictId,
      PolicyName expectedPolicyName) {
    assertEquals(1, result.conflicts().size());
    PolicyConflict conflict = result.conflicts().get(0);
    assertEquals(expectedPaoId, conflict.pao().getObjectId());
    assertEquals(expectedConflictId, conflict.conflictPao().getObjectId());
    assertEquals(expectedPolicyName, conflict.policyName());
  }

  private void checkForPolicies(Pao pao, PolicyInput... inputList) {
    PolicyInputs inputs = pao.getEffectiveAttributes();
    for (PolicyInput input : inputList) {
      var foundInput = inputs.lookupPolicy(input);
      assertNotNull(foundInput);
      assertEquals(input.getAdditionalData(), foundInput.getAdditionalData());
    }
  }

  private void checkForMissingPolicies(Pao pao, PolicyInput... inputList) {
    PolicyInputs inputs = pao.getEffectiveAttributes();
    for (PolicyInput input : inputList) {
      var foundInput = inputs.lookupPolicy(input);
      assertNull(foundInput);
    }
  }

  private PolicyInputs makePolicyInputs(PolicyInput... inputList) {
    var inputs = new PolicyInputs();
    for (PolicyInput input : inputList) {
      inputs.addInput(input);
    }
    return inputs;
  }

  private UUID makePao(PolicyInput... inputList) {
    var inputs = makePolicyInputs(inputList);
    UUID id = UUID.randomUUID();
    paoService.createPao(id, PaoComponent.WSM, PaoObjectType.WORKSPACE, inputs);
    return id;
  }

  private PolicyInput makeFlagInput(String name) {
    return PolicyInput.createFromMap(TEST_NAMESPACE, name, Collections.emptyMap());
  }

  private PolicyInput makeDataInput(String name, String data) {
    return PolicyInput.createFromMap(
        TEST_NAMESPACE, name, Collections.singletonMap(DATA_KEY, data));
  }
}
