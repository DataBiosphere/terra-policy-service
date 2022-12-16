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
  private static final String AZURE_PLATFORM = "azure";

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
    assertTrue(regionService.regionContainsDatacenter("usa", "gcp.us-central1"));
  }

  @Test
  void regionContainsDatacenterNegative() {
    assertFalse(regionService.regionContainsDatacenter("usa", "gcp.europe-west1"));
  }

  @Test
  void getDatacentersForRegionsInvalidRegion() {
    var result = regionService.getDataCentersForRegion("invalid", GCP_PLATFORM);
    assertEquals(0, result.size());
  }

  @Test
  void getDatacentersForRegionsAzureFilter() {
    var result = regionService.getDataCentersForRegion("global", AZURE_PLATFORM);
    assertEquals(0, result.size());
  }

  @Test
  void getDatacentersForRegionsGlobal() {
    var result = regionService.getDataCentersForRegion("global", GCP_PLATFORM);
    assertTrue(result.size() > 10);
  }

  @Test
  void getOntologyInvalidRegionName() {
    assertNull(regionService.getOntology("invalid", GCP_PLATFORM));
  }

  @Test
  void getOntologyFiltersByAzurePlatform() {
    var result = regionService.getOntology("gcp.us-central1", AZURE_PLATFORM);
    assertEquals(0, result.getDatacenters().length);
  }

  @Test
  void getOntologyFiltersByGcpPlatform() {
    var result = regionService.getOntology("gcp.us-central1", GCP_PLATFORM);
    assertEquals(1, result.getDatacenters().length);
  }

  @Test
  void getPaoDatacentersFromSelf() {
    var targetDatacenter = "europe-west3";
    var targetRegion = GCP_PLATFORM + "." + targetDatacenter;

    var pao = createPao(targetRegion);
    var datacenters =
        regionService.getPolicyInputDataCenterCodes(pao.getEffectiveAttributes(), GCP_PLATFORM);

    assertTrue(datacenters.contains(targetDatacenter));
  }

  @Test
  void getPaoDatacentersFromChild() {
    var region = "usa";
    var childDatacenter = "us-central1";

    var pao = createPao(region);
    var datacenters =
        regionService.getPolicyInputDataCenterCodes(pao.getEffectiveAttributes(), GCP_PLATFORM);

    assertTrue(datacenters.size() > 1);
    assertTrue(datacenters.contains(childDatacenter));
  }

  @Test
  void getPaoDatacentersNegative() {
    var region = "usa";
    var childDatacenterCode = "europe-west3";

    var pao = createPao(region);
    var datacenters =
        regionService.getPolicyInputDataCenterCodes(pao.getEffectiveAttributes(), GCP_PLATFORM);

    assertTrue(datacenters.size() > 1);
    assertFalse(datacenters.contains(childDatacenterCode));
  }

  @Test
  void getPaoDatacentersAllowAll() {
    // Create a PAO without a region constraint
    var objectId = UUID.randomUUID();
    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, new PolicyInputs());
    var pao = paoService.getPao(objectId);

    var datacenters =
        regionService.getPolicyInputDataCenterCodes(pao.getEffectiveAttributes(), GCP_PLATFORM);

    // Pao should be allowed all datacenters
    assertTrue(datacenters.size() > 10);
    assertTrue(datacenters.contains("us-east1"));
    assertTrue(datacenters.contains("europe-west3"));
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

  void isSubregionNegative() {
    // 'japan' is not a subregion of 'usa'
    assertFalse(regionService.isSubregion("usa", "japan"));
  }

  @Test
  void isSubregionUnknownParent() {
    // 'unknown' is not in the ontology, it's an invalid parent
    assertFalse(regionService.isSubregion("unknown", "global"));
  }

  @Test
  void isSubregionUnknownChild() {
    // 'unknown' is not in the ontology, it's an invalid child
    assertFalse(regionService.isSubregion("global", "unknown"));
  }

  @Test
  void isSubregionSelf() {
    // "global" should not be a subregion of itself
    assertFalse(regionService.isSubregion("global", "global"));
  }

  @Test
  void isSubregionChild() {
    // "europe" is a child of "global"
    assertTrue(regionService.isSubregion("global", "europe"));
  }

  @Test
  void isSubregionGrandchild() {
    // "finland" is a grandchild of "global"
    assertTrue(regionService.isSubregion("global", "finland"));
  }
}
