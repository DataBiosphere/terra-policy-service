package bio.terra.policy.service.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.policy.service.region.model.Datacenter;
import bio.terra.policy.service.region.model.Region;
import bio.terra.policy.testutils.TestUnitBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RegionServiceTest extends TestUnitBase {

  @Autowired private RegionService regionService;

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
}
