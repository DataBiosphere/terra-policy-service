package bio.terra.policy.service.region;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.PaoService;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DatacenterEvaluateTest extends TestUnitBase {
  private static final String TERRA = "terra";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String REGION = "region";
  private static final String US_REGION = "usa";

  @Autowired private PaoService paoService;
  @Autowired private RegionService regionService;

  @Test
  void policyAllowsDatacenter() {
    UUID paoId = UUID.randomUUID();
    createRegionConstrainedPao(paoId, US_REGION);
    Pao pao = paoService.getPao(paoId);

    assertTrue(regionService.paoAllowsDatacenter(pao, "us-central1", "gcp"));
  }

  @Test
  void policyDeniesDatacenter() {
    UUID paoId = UUID.randomUUID();
    createRegionConstrainedPao(paoId, US_REGION);
    Pao pao = paoService.getPao(paoId);

    assertFalse(regionService.paoAllowsDatacenter(pao, "europe-west3", "gcp"));
  }

  @Test
  void policyWithoutConstraintAllowsDatacenter() {
    UUID paoId = UUID.randomUUID();
    paoService.createPao(paoId, PaoComponent.WSM, PaoObjectType.WORKSPACE, new PolicyInputs());
    Pao pao = paoService.getPao(paoId);

    assertTrue(regionService.paoAllowsDatacenter(pao, "europe-west3", "gcp"));
    assertTrue(regionService.paoAllowsDatacenter(pao, "us-central1", "gcp"));
    assertTrue(regionService.paoAllowsDatacenter(pao, "southcentralus", "azure"));
  }

  private void createRegionConstrainedPao(UUID objectId, String region) {
    var regionPolicy =
        PolicyInput.createFromMap(
            TERRA, REGION_CONSTRAINT, Collections.singletonMap(REGION, region));

    var inputs = new PolicyInputs();
    inputs.addInput(regionPolicy);

    // Create a PAO
    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, inputs);
  }
}
