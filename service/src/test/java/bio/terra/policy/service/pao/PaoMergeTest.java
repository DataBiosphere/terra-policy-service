package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.model.PolicyInput;
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

public class PaoMergeTest extends TestUnitBase {
  private static final Logger logger = LoggerFactory.getLogger(PaoMergeTest.class);

  @Autowired private PaoService paoService;

  @Test
  void mergeSourceToEmptyDestination() {
    UUID paoSourceId =
        PaoTestUtil.makePao(
            paoService,
            PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
            PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
    logger.info("paoSourceId: {}", paoSourceId);

    UUID paoDestinationId = PaoTestUtil.makePao(paoService);
    logger.info("paoDestinationId: {}", paoDestinationId);

    // merge source to destination - destination should get all source policies
    PolicyUpdateResult result =
        paoService.mergeFromPao(paoSourceId, paoDestinationId, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("merge source to destination result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
  }

  @Test
  void mergeSourceToDestinationNoConflicts() {
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

    // merge A into B
    PolicyUpdateResult result =
        paoService.mergeFromPao(paoAid, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("merge A to B result: {}", result);
    assertTrue(result.conflicts().isEmpty());
    Pao resultPao = result.computedPao();
    PaoTestUtil.checkForPolicies(
        resultPao,
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A),
        PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B),
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1));
  }

  @Test
  void mergeSourceToDestinationAttributeConflict() {
    PolicyInput xData1 =
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1);
    PolicyInput xData2 =
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2);

    // PaoA - has flag A and data policy X, data1
    UUID paoAid =
        PaoTestUtil.makePao(
            paoService, PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_A), xData1);
    logger.info("paoAid: {}", paoAid);

    // PaoB - has flag B and data policy X, data1
    UUID paoBid =
        PaoTestUtil.makePao(
            paoService, PaoTestUtil.makeFlagInput(PaoTestUtil.TEST_FLAG_POLICY_B), xData2);
    logger.info("paoBid: {}", paoBid);

    // merge A into B
    PolicyUpdateResult result =
        paoService.mergeFromPao(paoAid, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    logger.info("merge A to B result: {}", result);
    PaoTestUtil.checkConflictFind(result, paoBid, paoAid, xData1.getPolicyName());
  }

  @Test
  void mergeSourceToDestinationSourceConflict() {
    // Source is empty - gets A as source with xData1
    // Destination is empty - gets B as source with xData2
    PolicyInput xData1 =
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA1);
    PolicyInput xData2 =
        PaoTestUtil.makeDataInput(PaoTestUtil.TEST_DATA_POLICY_X, PaoTestUtil.DATA2);

    UUID paoAid = PaoTestUtil.makePao(paoService, xData1);
    UUID paoBid = PaoTestUtil.makePao(paoService, xData2);
    UUID paoSourceId = PaoTestUtil.makePao(paoService);
    UUID paoDestinationId = PaoTestUtil.makePao(paoService);

    paoService.linkSourcePao(paoSourceId, paoAid, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(paoDestinationId, paoBid, PaoUpdateMode.FAIL_ON_CONFLICT);
    PaoTestUtil.checkForPolicies(paoService.getPao(paoSourceId), xData1);
    PaoTestUtil.checkForPolicies(paoService.getPao(paoDestinationId), xData2);

    // Merging should get a conflict
    PolicyUpdateResult result =
        paoService.mergeFromPao(paoSourceId, paoDestinationId, PaoUpdateMode.FAIL_ON_CONFLICT);
    PaoTestUtil.checkConflictUnknownId(result, paoDestinationId, xData1.getPolicyName());
  }
}
