package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.service.policy.model.PolicyUpdateResult;
import bio.terra.policy.testutils.PaoTestUtil;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PaoReplaceTest extends TestUnitBase {
  private static final Logger logger = LoggerFactory.getLogger(PaoReplaceTest.class);

  @Autowired private PaoService paoService;

  @Test
  void replaceSimple() {
    UUID paoId = PaoTestUtil.makePao(paoService);
    logger.info("paoId: {}", paoId);

    var newAttributes =
        PaoTestUtil.makePolicyInputs(
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    PolicyUpdateResult result =
        paoService.replacePao(paoId, newAttributes, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("replace result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    var moreNewAttributes =
        PaoTestUtil.makePolicyInputs(
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2));

    result = paoService.replacePao(paoId, moreNewAttributes, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("replace2 result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2));
  }

  @Test
  void replaceConflict() {
    // PaoA - has flag A and data policy X, data1
    UUID paoAid =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    logger.info("paoAid: {}", paoAid);

    // PaoB - starts empty
    UUID paoBid = PaoTestUtil.makePao(paoService);
    logger.info("paoBid: {}", paoBid);

    // Make A a source for B
    PolicyUpdateResult result =
        paoService.linkSourcePao(paoBid, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("merge A to B result: {}", result);
    assertTrue(result.updateApplied());
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));

    // Try a conflicting replace on A
    var newAttributes =
        PaoTestUtil.makePolicyInputs(
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2));
    result = paoService.replacePao(paoBid, newAttributes, PaoUpdateMode.FAIL_ON_CONFLICT);
    assertFalse(result.updateApplied());
    PaoTestUtil.checkConflict(
        result,
        paoBid,
        paoBid,
        new PolicyName(PaoTestUtil.TEST_NAMESPACE, PaoTestUtil.TEST_DATA_POLICY_X));
  }
}
