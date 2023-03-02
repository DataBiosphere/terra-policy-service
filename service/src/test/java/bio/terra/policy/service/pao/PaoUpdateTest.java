package bio.terra.policy.service.pao;

import static bio.terra.policy.testutils.PaoTestUtil.DEFAULT_REGION_NAME;
import static bio.terra.policy.testutils.PaoTestUtil.GROUP_CONSTRAINT;
import static bio.terra.policy.testutils.PaoTestUtil.GROUP_KEY;
import static bio.terra.policy.testutils.PaoTestUtil.GROUP_NAME;
import static bio.terra.policy.testutils.PaoTestUtil.REGION_CONSTRAINT;
import static bio.terra.policy.testutils.PaoTestUtil.REGION_KEY;
import static bio.terra.policy.testutils.PaoTestUtil.TERRA_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.exception.DirectConflictException;
import bio.terra.policy.common.exception.IllegalCycleException;
import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.service.policy.PolicyMutator;
import bio.terra.policy.service.policy.model.PolicyUpdateResult;
import bio.terra.policy.testutils.PaoTestUtil;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PaoUpdateTest extends TestUnitBase {
  private static final Logger logger = LoggerFactory.getLogger(PaoUpdateTest.class);

  @Autowired private PaoService paoService;

  @Test
  void unknownFlagCombinerTest() throws Exception {
    // Flag policies refer to unknown policies that have no additional data.
    // They act like flags. If anyone sets the flag, it is set.

    var flagPolicyA1 = PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A);
    var flagPolicyA2 = PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A);

    // Combining two of the same flag should result in the A flag being set.
    PolicyInput flagResult = PolicyMutator.combine(flagPolicyA1, flagPolicyA2);
    Assertions.assertEquals(flagResult.getKey(), flagPolicyA1.getKey());
    assertEquals(0, flagResult.getAdditionalData().size());

    // It is an error to combine policies with different names. This is an internal
    // error, because in the production code, combiner check should never happen.
    var flagPolicyB = PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B);
    assertThrows(
        InternalTpsErrorException.class, () -> PolicyMutator.combine(flagPolicyA1, flagPolicyB));
  }

  @Test
  void unknownDataCombinerTest() throws Exception {
    // The term "data policy" here refers to an unknown policy with additional data.
    // The rule in the PolicyUnknown implementation is that if the data is different
    // the policies conflict.
    var data1Policy1 = PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1);
    var data1Policy2 = PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1);

    // Combining identical data policies should result in a single data policy with the same
    // additional data
    PolicyInput dataResult = PolicyMutator.combine(data1Policy1, data1Policy2);
    Assertions.assertEquals(dataResult.getKey(), data1Policy1.getKey());
    Assertions.assertEquals(dataResult.getAdditionalData(), data1Policy1.getAdditionalData());

    // Combining the same policy name with different additional data should be a conflict.
    // The combiner reports conflicts by returning null.
    var data2Policy = PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2);
    dataResult = PolicyMutator.combine(data1Policy1, data2Policy);
    assertNull(dataResult);
  }

  @Test
  void linkSourceToEmptyTest_policiesPropagateCorrectly() throws Exception {
    // PaoA - has flag A and data policy X, data1
    UUID paoAid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    logger.info("paoAid: {}", paoAid);

    // PaoB - has flag B and data policy X, data1
    UUID paoBid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    logger.info("paoBid: {}", paoBid);

    // PaoC - has data policy Y, data 1
    UUID paoCid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA1));
    logger.info("paoCid: {}", paoCid);

    // PaoD - has data policy Y, data2
    UUID paoDid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA2));
    logger.info("paoDid: {}", paoDid);

    // PaoNone - has no policies
    UUID paoNone = PaoTestUtil.makePao(paoService);
    logger.info("paoNone: {}", paoNone);

    // Link A to None - none should get flag A and data policy X, data1
    PolicyUpdateResult result =
        paoService.linkSourcePao(paoNone, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link A to None result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    // Link B to None - none should get flag A, flag B, data policy X, data1
    result = paoService.linkSourcePao(paoNone, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link B to None result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoBid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    // Link C to None - none should get flag A, flag B, data policy X, data1
    // and data policy Y data 1
    result = paoService.linkSourcePao(paoNone, paoCid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link C to None result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoBid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA1));

    // Link D to None - none should stay in previous state with data policy Y with a conflict
    result = paoService.linkSourcePao(paoNone, paoDid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link D to None result: {}", result);
    assertFalse(result.updateApplied());
    PaoTestUtil.checkConflict(
        result,
        paoNone,
        paoDid,
        new PolicyName(PaoTestUtil.TEST_NAMESPACE, PaoTestUtil.TEST_DATA_POLICY_Y));

    // Same comparison as with Link C to None
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoBid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA1));

    // Link C to D - D should stay the same with data policy Y in conflict
    result = paoService.linkSourcePao(paoDid, paoCid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link C to D result: {}", result);
    assertFalse(result.updateApplied());
    PaoTestUtil.checkConflict(
        result,
        paoDid,
        paoCid,
        new PolicyName(PaoTestUtil.TEST_NAMESPACE, PaoTestUtil.TEST_DATA_POLICY_Y));

    // In this case, the resultPao is showing the failed state: with the linked source in place.
    // NOTE: Is that the right thing? I think so: it provides the details of policies that would be
    // included in the dependent that did not conflict.
    // The alternative would be to return the initialPao if the operation fails.
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    PaoTestUtil.checkForPolicies(
        resultPao, PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA2));

    // While we are here, make sure the invalid input case is caught.
    assertThrows(
        InternalTpsErrorException.class,
        () -> paoService.linkSourcePao(paoDid, paoCid, PaoUpdateMode.ENFORCE_CONFLICTS));
  }

  @Test
  void linkSourceHierarchyTest_policesCombineCorrectly() throws Exception {
    // PaoA - has flag A and data policy X, data1
    UUID paoAid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    logger.info("paoAid: {}", paoAid);

    // PaoB - has flag B and data policy X, data1
    UUID paoBid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    logger.info("paoBid: {}", paoBid);

    // PaoC - has data policy Y, data 1
    UUID paoCid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA1));
    logger.info("paoCid: {}", paoCid);

    // PaoD - has data policy Y, data2
    UUID paoDid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA2));
    logger.info("paoDid: {}", paoDid);

    // PaoNone - has no policies
    UUID paoNone = PaoTestUtil.makePao(paoService);
    logger.info("paoNone: {}", paoNone);

    // Link A to B
    PolicyUpdateResult result =
        paoService.linkSourcePao(paoBid, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link A to B result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    // Link A to C
    result = paoService.linkSourcePao(paoCid, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link A to C result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA1));

    // Link B to C
    result = paoService.linkSourcePao(paoCid, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link B to C result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoAid));
    assertTrue(resultPao.getSourceObjectIds().contains(paoBid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA1));

    // Link C to None
    // and data policy Y data 1
    result = paoService.linkSourcePao(paoNone, paoCid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link C to None result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA1));

    // Link D to None - none should stay in previous state with data policy Y with a conflict
    result = paoService.linkSourcePao(paoNone, paoDid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Link D to None result: {}", result);
    // PaoTestUtil.check that we got the right conflict
    assertFalse(result.updateApplied());
    assertEquals(1, result.conflicts().size());
    PolicyConflict conflict = result.conflicts().get(0);
    assertEquals(paoNone, conflict.pao().getObjectId());
    assertEquals(paoDid, conflict.conflictPao().getObjectId());
    assertEquals(
        new PolicyName(PaoTestUtil.TEST_NAMESPACE, PaoTestUtil.TEST_DATA_POLICY_Y),
        conflict.policyName());
    // PaoTestUtil.check that None remains unchanged
    resultPao = result.computedPao();
    assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_Y, PaoTestUtil.DATA1));

    // Link None to A - should fail because it would create a cycle
    assertThrows(
        IllegalCycleException.class,
        () -> paoService.linkSourcePao(paoAid, paoNone, PaoUpdateMode.FAIL_ON_CONFLICT));
  }

  @Test
  void updateSinglePaoTest_addDifferentConstraintType() throws Exception {
    PolicyInput regionPolicy =
        PolicyInput.createFromMap(
            TERRA_NAMESPACE,
            REGION_CONSTRAINT,
            Collections.singletonMap(REGION_KEY, DEFAULT_REGION_NAME));
    PolicyInput groupPolicy =
        PolicyInput.createFromMap(
            TERRA_NAMESPACE, GROUP_CONSTRAINT, Collections.singletonMap(GROUP_KEY, GROUP_NAME));

    UUID paoId = PaoTestUtil.makePao(paoService, regionPolicy);
    logger.info("make pao {} with region policy", paoId);

    PolicyInputs groupPolicyInput = PaoTestUtil.makePolicyInputs(groupPolicy);

    PolicyUpdateResult result =
        paoService.updatePao(
            paoId, groupPolicyInput, new PolicyInputs(), PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 1 adds a group constraint. Result: {}", result);

    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(resultPao, regionPolicy, groupPolicy);
  }

  @Test
  void updateSinglePaoTest_paoAttributesUpdateCorrectly() throws Exception {
    // Basic updates on a single Pao
    // Pao has flag A and data policy X, data1
    UUID paoId =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    logger.info("paoId: {}", paoId);

    PolicyInputs removes =
        PaoTestUtil.makePolicyInputs(
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    PolicyInputs adds =
        PaoTestUtil.makePolicyInputs(
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A));

    PolicyUpdateResult result =
        paoService.updatePao(paoId, adds, removes, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 1 removes {} adds {} result {}", removes, adds, result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B));
    PaoTestUtil.checkForMissingPolicies(
        resultPao, PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    // Repeating the operation should get the same result.
    result = paoService.updatePao(paoId, adds, removes, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 2 removes {} adds {} result {}", removes, adds, result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B));
    PaoTestUtil.checkForMissingPolicies(
        resultPao, PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    // Go from zero to one policy
    UUID paoNone = PaoTestUtil.makePao(paoService);
    logger.info("paoNone: {}", paoNone);
    PolicyInputs empty = PaoTestUtil.makePolicyInputs();
    PolicyInputs oneFlag =
        PaoTestUtil.makePolicyInputs(PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A));
    result = paoService.updatePao(paoNone, oneFlag, empty, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 3 removes {} adds {} result {}", removes, adds, result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(
        resultPao, PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A));

    // Go from one to zero policy
    result = paoService.updatePao(paoNone, empty, oneFlag, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Update 4 removes {} adds {} result {}", removes, adds, result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    assertTrue(resultPao.getEffectiveAttributes().getInputs().isEmpty());

    // Try to add a conflicting policy
    UUID paoConflict =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    logger.info("paoConflict: {}", paoConflict);
    PolicyInputs conflictPolicy =
        PaoTestUtil.makePolicyInputs(
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2));

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

    UUID paoSid = PaoTestUtil.makePao(paoService);
    logger.info("paoSid: {}", paoSid);
    UUID paoDid = PaoTestUtil.makePao(paoService);
    logger.info("paoDid: {}", paoDid);
    paoService.linkSourcePao(paoDid, paoSid, PaoUpdateMode.FAIL_ON_CONFLICT);

    PolicyInputs empty = PaoTestUtil.makePolicyInputs();
    PolicyInputs oneFlag =
        PaoTestUtil.makePolicyInputs(PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A));
    // Add the flag policy
    PolicyUpdateResult result =
        paoService.updatePao(paoSid, oneFlag, empty, PaoUpdateMode.FAIL_ON_CONFLICT);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    PaoTestUtil.checkForPolicies(
        result.computedPao(), PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A));

    // Double check that the policy got saved properly
    Pao checkD = paoService.getPao(paoDid);
    PaoTestUtil.checkForPolicies(checkD, PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A));

    // Remove the flag policy
    result = paoService.updatePao(paoSid, empty, oneFlag, PaoUpdateMode.FAIL_ON_CONFLICT);
    assertTrue(result.updateApplied());
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
    PolicyInput xData2 =
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2);

    UUID paoS1id = PaoTestUtil.makePao(paoService);
    logger.info("paoS1id: {}", paoS1id);
    UUID paoS2id = PaoTestUtil.makePao(paoService, xData2);
    logger.info("paoS2id: {}", paoS2id);
    UUID paoDid = PaoTestUtil.makePao(paoService);
    logger.info("paoDid: {}", paoDid);
    paoService.linkSourcePao(paoDid, paoS1id, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(paoDid, paoS2id, PaoUpdateMode.FAIL_ON_CONFLICT);

    // Check that the dependent has the right policy
    Pao checkD = paoService.getPao(paoDid);
    PaoTestUtil.checkForPolicies(checkD, xData2);

    // Conflict case - DRY_RUN
    PolicyInputs empty = PaoTestUtil.makePolicyInputs();
    PolicyInputs conflictPolicy =
        PaoTestUtil.makePolicyInputs(
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    PolicyUpdateResult result =
        paoService.updatePao(paoS1id, conflictPolicy, empty, PaoUpdateMode.DRY_RUN);
    assertFalse(result.updateApplied());
    PaoTestUtil.checkConflict(
        result,
        paoDid,
        paoS1id,
        new PolicyName(PaoTestUtil.TEST_NAMESPACE, PaoTestUtil.TEST_DATA_POLICY_X));
    checkD = paoService.getPao(paoDid);
    PaoTestUtil.checkForPolicies(checkD, xData2);
    PolicyInput conflicted = checkD.getEffectiveAttributes().getInputs().get(xData2.getKey());
    assertEquals(0, conflicted.getConflicts().size());

    // Conflict case - FAIL_ON_CONFLICT
    // should have the same result as DRY_RUN, since there is a conflict
    result = paoService.updatePao(paoS1id, conflictPolicy, empty, PaoUpdateMode.FAIL_ON_CONFLICT);
    assertFalse(result.updateApplied());
    PaoTestUtil.checkConflict(
        result,
        paoDid,
        paoS1id,
        new PolicyName(PaoTestUtil.TEST_NAMESPACE, PaoTestUtil.TEST_DATA_POLICY_X));
    checkD = paoService.getPao(paoDid);
    PaoTestUtil.checkForPolicies(checkD, xData2);
    conflicted = checkD.getEffectiveAttributes().getInputs().get(xData2.getKey());
    assertEquals(0, conflicted.getConflicts().size());

    // Conflict case - ENFORCE_CONFLICTS
    result = paoService.updatePao(paoS1id, conflictPolicy, empty, PaoUpdateMode.ENFORCE_CONFLICTS);
    assertTrue(result.updateApplied());
    PaoTestUtil.checkConflict(
        result,
        paoDid,
        paoS1id,
        new PolicyName(PaoTestUtil.TEST_NAMESPACE, PaoTestUtil.TEST_DATA_POLICY_X));
    checkD = paoService.getPao(paoDid);
    PaoTestUtil.checkForPolicies(checkD, xData2);

    conflicted = checkD.getEffectiveAttributes().getInputs().get(xData2.getKey());
    assertEquals(1, conflicted.getConflicts().size());
    assertTrue(conflicted.getConflicts().contains(paoS1id));
  }

  @Test
  void updatePropagateConflictTest_dependentsConflict() throws Exception {
    // We build this graph A --> B(X-data1) --> C --> D
    // Then we attempt to update A(X-data2) for DRY_RUN, FAIL_ON_CONFLICT, and ENFORCE_CONFLICTS
    // We expect the conflict to propagate from B to C and D.
    PolicyInput xData1 =
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1);
    PolicyInput xData2 =
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2);
    PolicyInputs conflictPolicy = PaoTestUtil.makePolicyInputs(xData2);
    PolicyInputs emptyPolicy = PaoTestUtil.makePolicyInputs();

    UUID paoAid = PaoTestUtil.makePao(paoService);
    UUID paoBid = PaoTestUtil.makePao(paoService, xData1);
    UUID paoCid = PaoTestUtil.makePao(paoService);
    UUID paoDid = PaoTestUtil.makePao(paoService);

    // Hook up the graph and test the policies end up where we expect them
    paoService.linkSourcePao(paoBid, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(paoDid, paoCid, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(paoCid, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    PaoTestUtil.checkForPolicies(paoService.getPao(paoDid), xData1);
    PaoTestUtil.checkForPolicies(paoService.getPao(paoCid), xData1);
    PaoTestUtil.checkForPolicies(paoService.getPao(paoBid), xData1);
    PaoTestUtil.checkForMissingPolicies(paoService.getPao(paoAid), xData1);

    PolicyUpdateResult result =
        paoService.updatePao(paoAid, conflictPolicy, emptyPolicy, PaoUpdateMode.DRY_RUN);
    logger.info("Result: {}", result);
    assertFalse(result.updateApplied());
    assertEquals(3, result.conflicts().size()); // B, C, and D
    PaoTestUtil.checkConflictFind(result, paoBid, paoAid, xData1.getPolicyName());
    PaoTestUtil.checkConflictFind(result, paoCid, paoBid, xData1.getPolicyName());
    PaoTestUtil.checkConflictFind(result, paoDid, paoCid, xData1.getPolicyName());

    result =
        paoService.updatePao(paoAid, conflictPolicy, emptyPolicy, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("Result: {}", result);
    assertFalse(result.updateApplied());
    assertEquals(3, result.conflicts().size()); // B, C, and D
    PaoTestUtil.checkConflictFind(result, paoBid, paoAid, xData1.getPolicyName());
    PaoTestUtil.checkConflictFind(result, paoCid, paoBid, xData1.getPolicyName());
    PaoTestUtil.checkConflictFind(result, paoDid, paoCid, xData1.getPolicyName());

    result =
        paoService.updatePao(paoAid, conflictPolicy, emptyPolicy, PaoUpdateMode.ENFORCE_CONFLICTS);
    logger.info("Result: {}", result);
    assertTrue(result.updateApplied());
    assertEquals(3, result.conflicts().size()); // B, C, and D
    PaoTestUtil.checkConflictFind(result, paoBid, paoAid, xData1.getPolicyName());
    PaoTestUtil.checkConflictFind(result, paoCid, paoBid, xData1.getPolicyName());
    PaoTestUtil.checkConflictFind(result, paoDid, paoCid, xData1.getPolicyName());
  }
}
