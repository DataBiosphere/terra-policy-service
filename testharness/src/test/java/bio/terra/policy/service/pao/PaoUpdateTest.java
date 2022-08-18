package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.service.policy.PolicyCombiner;
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
    var flagPolicyA1 = makeFlagInput(TEST_FLAG_POLICY_A);
    var flagPolicyA2 = makeFlagInput(TEST_FLAG_POLICY_A);

    PolicyInput flagResult = PolicyCombiner.combine(flagPolicyA1, flagPolicyA2);
    assertEquals(flagResult.getKey(), flagPolicyA1.getKey());
    assertEquals(0, flagResult.getAdditionalData().size());

    var flagPolicyB = makeFlagInput(TEST_FLAG_POLICY_B);

    assertThrows(
        InternalTpsErrorException.class, () -> PolicyCombiner.combine(flagPolicyA1, flagPolicyB));
  }

  @Test
  void unknownDataCombinerTest() throws Exception {
    // matching test
    var data1Policy1 = makeDataInput(TEST_DATA_POLICY_X, DATA1);
    var data1Policy2 = makeDataInput(TEST_DATA_POLICY_X, DATA1);

    PolicyInput dataResult = PolicyCombiner.combine(data1Policy1, data1Policy2);
    assertEquals(dataResult.getKey(), data1Policy1.getKey());
    assertEquals(dataResult.getAdditionalData(), data1Policy1.getAdditionalData());

    // conflict test
    var data2Policy = makeDataInput(TEST_DATA_POLICY_X, DATA2);
    dataResult = PolicyCombiner.combine(data1Policy1, data2Policy);
    assertNull(dataResult);
  }

  @Test
  void linkSourceToEmptyTest() throws Exception {
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
    assertEquals(1, result.conflicts().size());
    PolicyConflict conflict = result.conflicts().get(0);
    assertEquals(paoNone, conflict.dependent().getObjectId());
    assertEquals(paoDid, conflict.source().getObjectId());
    assertEquals(new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_Y), conflict.policyName());

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
    assertEquals(1, result.conflicts().size());
    conflict = result.conflicts().get(0);
    assertEquals(paoDid, conflict.dependent().getObjectId());
    assertEquals(paoCid, conflict.source().getObjectId());
    assertEquals(new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_Y), conflict.policyName());

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
  void linkSourceHierarchyTest() throws Exception {
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
    assertEquals(1, result.conflicts().size());
    PolicyConflict conflict = result.conflicts().get(0);
    assertEquals(paoNone, conflict.dependent().getObjectId());
    // NOTE: The conflict source can be either C or D depending on the order of evaluation.
    // If D is walked first, then it is the "good" policy and C is the conflict.
    // If C is walked first, then is is the "good" policy and D is the conflict.
    // Is that the right thing? An alternative is to sequence the walking, so we walk all existing
    // sources first and then the new source. The crux is the best way to express the conflict;
    // Ideally we would say "policy X (with data A) from (PAO: could be the attributes of the node
    // or
    // the source), conflicts with policy X from (ditto).
    assertTrue(
        conflict.source().getObjectId().equals(paoCid)
            || conflict.source().getObjectId().equals(paoDid));
    assertEquals(new PolicyName(TEST_NAMESPACE, TEST_DATA_POLICY_Y), conflict.policyName());

    // Same comparison as with Link C to None
    /*
    // Because of the ordering issue above, the resulting policies are not reliably unchanged.
        resultPao = result.computedPao();
        assertTrue(resultPao.getSourceObjectIds().contains(paoCid));
        checkForPolicies(
            resultPao,
            makeFlagInput(TEST_FLAG_POLICY_A),
            makeFlagInput(TEST_FLAG_POLICY_B),
            makeDataInput(TEST_DATA_POLICY_X, DATA1),
            makeDataInput(TEST_DATA_POLICY_Y, DATA1));

     */
  }

  private void checkForPolicies(Pao pao, PolicyInput... inputList) {
    PolicyInputs inputs = pao.getEffectiveAttributes();
    for (PolicyInput input : inputList) {
      var foundInput = inputs.lookupPolicy(input);
      assertNotNull(foundInput);
      assertEquals(input.getAdditionalData(), foundInput.getAdditionalData());
    }
  }

  private UUID makePao(PolicyInput... inputList) {
    var inputs = new PolicyInputs();
    for (PolicyInput input : inputList) {
      inputs.addInput(input);
    }
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
