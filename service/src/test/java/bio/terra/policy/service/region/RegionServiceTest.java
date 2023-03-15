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
import bio.terra.policy.service.region.model.Location;
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
  void getLocation() {
    String searchRegion = "gcp.europe-west3";
    Location result = regionService.getLocation(searchRegion);
    assertNotNull(result);
    assertEquals(searchRegion, result.getName());
  }

  @Test
  void getLocationInvalidRegionReturnsNull() {
    String searchRegion = "invalid";
    Location result = regionService.getLocation(searchRegion);
    assertNull(result);
  }

  @Test
  void getRegion() {
    String searchName = "gcp.us-central1";
    Region result = regionService.getRegion(searchName);
    assertNotNull(result);
    assertEquals(searchName, result.getId());
  }

  @Test
  void getRegionInvalidIdReturnsNull() {
    String searchName = "invalid";
    Region result = regionService.getRegion(searchName);
    assertNull(result);
  }

  @Test
  void locationContainsRegionFromItself() {
    assertTrue(regionService.locationContainsRegion("europe", "gcp.europe-west1"));
  }

  @Test
  void locationContainsRegionFromSubRegion() {
    assertTrue(regionService.locationContainsRegion("usa", "gcp.us-central1"));
  }

  @Test
  void locationContainsRegionNegative() {
    assertFalse(regionService.locationContainsRegion("usa", "gcp.europe-west1"));
  }

  @Test
  void getRegionsForLocationInvalidRegion() {
    var result = regionService.getRegionsForLocation("invalid", GCP_PLATFORM);
    assertNull(result);
  }

  @Test
  void getRegionsForLocationEmptyRegion() {
    var result = regionService.getRegionsForLocation("", GCP_PLATFORM);
    assertNotNull(result);
    assertTrue(result.size() > 1);
  }

  @Test
  void getRegionsForLocationNullRegion() {
    var result = regionService.getRegionsForLocation(null, GCP_PLATFORM);
    assertNotNull(result);
    assertTrue(result.size() > 1);
  }

  @Test
  void getRegionsForLocationAzureFilter() {
    var result = regionService.getRegionsForLocation("global", AZURE_PLATFORM);
    assertEquals(25, result.size());
  }

  @Test
  void getRegionsForLocationGlobal() {
    var result = regionService.getRegionsForLocation("global", GCP_PLATFORM);
    assertTrue(result.size() > 10);
  }

  @Test
  void getOntologyGlobalRegion() {
    var result = regionService.getOntology("global", GCP_PLATFORM);
    assertNotNull(result);
  }

  @Test
  void getOntologyEmptyRegion() {
    var result = regionService.getOntology("", GCP_PLATFORM);
    assertNotNull(result);
  }

  @Test
  /** As an optional query param for the primary API caller, the name might be null. * */
  void getOntologyNullRegion() {
    var result = regionService.getOntology(null, GCP_PLATFORM);
    assertNotNull(result);
  }

  @Test
  void getOntologyChildRegion() {
    var result = regionService.getOntology("usa", GCP_PLATFORM);
    assertNotNull(result);
  }

  @Test
  void getOntologyInvalidRegionName() {
    assertNull(regionService.getOntology("invalid", GCP_PLATFORM));
  }

  @Test
  void getOntologyFiltersByAzurePlatform() {
    var result = regionService.getOntology("gcp.us-central1", AZURE_PLATFORM);
    assertEquals(0, result.getRegions().length);
  }

  @Test
  void getOntologyFiltersByGcpPlatform() {
    var result = regionService.getOntology("gcp.us-central1", GCP_PLATFORM);
    assertEquals(1, result.getRegions().length);
  }

  @Test
  void getPolicyInputRegionCodesFromSelf() {
    var targetRegion = GCP_PLATFORM + ".europe-west3";

    var pao = createPao(targetRegion);
    var actual = regionService.getPolicyInputRegions(pao.getEffectiveAttributes(), GCP_PLATFORM);

    assertTrue(actual.stream().anyMatch(r -> r.getId().equals(targetRegion)));
  }

  @Test
  void getPolicyInputRegionCodesFromChild() {
    var region = "usa";
    var childRegion = "us-central1";

    var pao = createPao(region);
    var regions = regionService.getPolicyInputRegions(pao.getEffectiveAttributes(), GCP_PLATFORM);

    assertTrue(regions.size() > 1);
    assertTrue(regions.stream().anyMatch(r -> r.getCode().equals(childRegion)));
  }

  @Test
  void getPolicyInputRegionCodesNegative() {
    var region = "usa";
    var childRegionCode = "europe-west3";

    var pao = createPao(region);
    var regions = regionService.getPolicyInputRegions(pao.getEffectiveAttributes(), GCP_PLATFORM);

    assertTrue(regions.size() > 1);
    assertFalse(regions.contains(childRegionCode));
  }

  @Test
  void getPolicyInputRegionCodesAllowAll() {
    // Create a PAO without a region constraint
    var objectId = UUID.randomUUID();
    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, new PolicyInputs());
    var pao = paoService.getPao(objectId);

    var regions = regionService.getPolicyInputRegions(pao.getEffectiveAttributes(), GCP_PLATFORM);

    // Pao should be allowed all regions
    assertTrue(regions.size() > 10);
    assertTrue(regions.stream().anyMatch(r -> r.getCode().equals("us-east1")));
    assertTrue(regions.stream().anyMatch(r -> r.getCode().equals("europe-west3")));
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

  @Test
  void isSubLocationNegative() {
    // 'japan' is not a subregion of 'usa'
    assertFalse(regionService.isSubLocation("usa", "japan"));
  }

  @Test
  void isSubLocationUnknownParent() {
    // 'unknown' is not in the ontology, it's an invalid parent
    assertFalse(regionService.isSubLocation("unknown", "global"));
  }

  @Test
  void isSubLocationUnknownChild() {
    // 'unknown' is not in the ontology, it's an invalid child
    assertFalse(regionService.isSubLocation("global", "unknown"));
  }

  @Test
  void isSubLocationSelf() {
    // "global" should not be a subregion of itself
    assertFalse(regionService.isSubLocation("global", "global"));
  }

  @Test
  void isSubLocationChild() {
    // "europe" is a child of "global"
    assertTrue(regionService.isSubLocation("global", "europe"));
  }

  @Test
  void isSubLocationGrandchild() {
    // "finland" is a grandchild of "global"
    assertTrue(regionService.isSubLocation("global", "finland"));
  }
}
