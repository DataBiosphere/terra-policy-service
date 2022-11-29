package bio.terra.policy.service.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.PaoService;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.region.model.Datacenter;
import bio.terra.policy.service.region.model.Region;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RegionServiceTest extends TestUnitBase {
  private static final String GCP_PLATFORM = "gcp";

  @Autowired private RegionService regionService;
  @Autowired private PaoService paoService;

  @Test
  void getRegion() {
    String searchRegion = "gcp.europe-west3";
    Region result = regionService.getRegion(searchRegion);
    assertNotNull(result);
    assertEquals(searchRegion, result.getName());
  }

  @Test
  void getRegionInvalidRegionReturnsNull() {
    String searchRegion = "invalid";
    Region result = regionService.getRegion(searchRegion);
    assertNull(result);
  }

  @Test
  void getDatacenter() {
    String searchName = "gcp.us-central1";
    Datacenter result = regionService.getDatacenter(searchName);
    assertNotNull(result);
    assertEquals(searchName, result.getId());
  }

  @Test
  void getDatacenterInvalidIdReturnsNull() {
    String searchName = "invalid";
    Datacenter result = regionService.getDatacenter(searchName);
    assertNull(result);
  }

  @Test
  void regionContainsDatacenterFromItself() {
    assertTrue(regionService.regionContainsDatacenter("europe", "gcp.europe-west1"));
  }

  @Test
  void regionContainsDatacenterFromSubRegion() {
    assertTrue(regionService.regionContainsDatacenter("usa", "azure.centralus"));
  }

  @Test
  void regionContainsDatacenterNegative() {
    assertFalse(regionService.regionContainsDatacenter("usa", "gcp.europe-west1"));
  }

  @Test
  void getPaoDatacentersFromSelf() {
    var target = "gcp.europe-west3";

    var pao = createPao(target);
    var datacenters = regionService.getPaoDatacenters(pao, GCP_PLATFORM);

    assertTrue(datacenters.contains(target));
  }

  @Test
  void getPaoDatacentersFromChild() {
    var region = "usa";
    var childDatacenter = "gcp.us-central1";

    var pao = createPao(region);
    var datacenters = regionService.getPaoDatacenters(pao, GCP_PLATFORM);

    assertTrue(datacenters.size() > 1);
    assertTrue(datacenters.contains(childDatacenter));
  }

  @Test
  void getPaoDatacentersNegative() {
    var region = "usa";
    var childDatacenter = "gcp.europe-west3";

    var pao = createPao(region);
    var datacenters = regionService.getPaoDatacenters(pao, GCP_PLATFORM);

    assertTrue(datacenters.size() > 1);
    assertFalse(datacenters.contains(childDatacenter));
  }

  @Test
  void getPaoDatacentersAllowAll() {
    // Create a PAO without a region constraint
    var objectId = UUID.randomUUID();
    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, new PolicyInputs());
    var pao = paoService.getPao(objectId);

    var datacenters = regionService.getPaoDatacenters(pao, GCP_PLATFORM);

    // Pao should be allowed all datacenters
    assertTrue(datacenters.size() == 1);
    assertTrue(datacenters.contains("*"));
  }

  private Pao createPao(String region) {
    var objectId = UUID.randomUUID();

    var regionPolicy =
        PolicyInput.createFromMap(
            "terra", "region-constraint", Collections.singletonMap("region-name", region));

    var inputs = new PolicyInputs();
    inputs.addInput(regionPolicy);

    // Create a PAO
    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, inputs);
    return paoService.getPao(objectId);
  }
}
