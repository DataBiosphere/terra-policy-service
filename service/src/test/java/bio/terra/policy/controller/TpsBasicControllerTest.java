package bio.terra.policy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import bio.terra.policy.generated.model.ApiTpsPolicyInput;
import bio.terra.policy.generated.model.ApiTpsPolicyInputs;
import bio.terra.policy.generated.model.ApiTpsPolicyPair;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TpsBasicControllerTest extends TestUnitBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String GROUP = "group";
  private static final String REGION = "region-name";
  private static final String DDGROUP = "ddgroup";
  private static final String US_REGION = "usa";
  private static final String IOWA_REGION = "iowa";

  @Autowired private MvcUtils mvcUtils;

  @Test
  public void basicPaoTest() throws Exception {
    var groupPolicy =
        new ApiTpsPolicyInput()
            .namespace(TERRA)
            .name(GROUP_CONSTRAINT)
            .addAdditionalDataItem(new ApiTpsPolicyPair().key(GROUP).value(DDGROUP));

    var regionPolicy =
        new ApiTpsPolicyInput()
            .namespace(TERRA)
            .name(REGION_CONSTRAINT)
            .addAdditionalDataItem(new ApiTpsPolicyPair().key(REGION).value(US_REGION));

    var inputs = new ApiTpsPolicyInputs().addInputsItem(groupPolicy);

    // Create a PAO
    UUID paoIdA = mvcUtils.createPao(inputs);
    String lastUpdatedA = checkInitialLastUpdate(paoIdA);

    // Create another PAO with a group policy
    UUID paoIdB = mvcUtils.createPao(new ApiTpsPolicyInputs().addInputsItem(regionPolicy));
    String lastUpdatedB = checkInitialLastUpdate(paoIdB);

    // Get a PAO
    var apiPao = mvcUtils.getPao(paoIdA);
    checkAttributeSet(apiPao.getAttributes(), US_REGION);
    checkAttributeSet(apiPao.getEffectiveAttributes(), US_REGION);

    // Merge a PAO
    var updateResult = mvcUtils.mergePao(paoIdB, paoIdA);
    assertTrue(updateResult.isUpdateApplied());
    assertEquals(0, updateResult.getConflicts().size());
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes(), US_REGION);
    checkLastUpdateChanged(lastUpdatedA, paoIdA);

    // Link a PAO
    updateResult = mvcUtils.linkPao(paoIdB, paoIdA);
    assertTrue(updateResult.isUpdateApplied());
    assertEquals(0, updateResult.getConflicts().size());
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes(), US_REGION);
    lastUpdatedB = checkLastUpdateChanged(lastUpdatedB, paoIdB);

    // need to work with region inputs since we can't update groups.
    var iowaRegionPolicy =
        new ApiTpsPolicyInput()
            .namespace(TERRA)
            .name(REGION_CONSTRAINT)
            .addAdditionalDataItem(new ApiTpsPolicyPair().key(REGION).value(IOWA_REGION));
    var iowaInputs = new ApiTpsPolicyInputs().addInputsItem(iowaRegionPolicy);

    // Update a PAO
    updateResult = mvcUtils.updatePao(paoIdB, iowaInputs);
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes(), IOWA_REGION);
    lastUpdatedB = checkLastUpdateChanged(lastUpdatedB, paoIdB);

    // Replace a PAO
    updateResult = mvcUtils.replacePao(paoIdB, iowaInputs);
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes(), IOWA_REGION);
    checkLastUpdateChanged(lastUpdatedB, paoIdB);

    // Delete a PAO
    mvcUtils.deletePao(paoIdA);
    mvcUtils.deletePao(paoIdB);
  }

  private void checkAttributeSet(ApiTpsPolicyInputs attributeSet, String expectedRegion) {
    for (ApiTpsPolicyInput attribute : attributeSet.getInputs()) {
      assertEquals(TERRA, attribute.getNamespace());
      assertEquals(1, attribute.getAdditionalData().size());

      if (attribute.getName().equals(GROUP_CONSTRAINT)) {
        assertEquals(GROUP, attribute.getAdditionalData().get(0).getKey());
        assertEquals(DDGROUP, attribute.getAdditionalData().get(0).getValue());
      } else if (attribute.getName().equals(REGION_CONSTRAINT)) {
        assertEquals(REGION, attribute.getAdditionalData().get(0).getKey());
        assertEquals(expectedRegion, attribute.getAdditionalData().get(0).getValue());
      } else {
        fail();
      }
    }
  }

  /** check that the initial Pao create matches lastUpdated and return lastUpdated */
  private String checkInitialLastUpdate(UUID paoId) throws Exception {
    var apiPao = mvcUtils.getPao(paoId);
    assertEquals(apiPao.getCreatedDate(), apiPao.getLastUpdatedDate());
    return apiPao.getLastUpdatedDate();
  }

  /** check that lastUpdated changed and return new lastUpdated */
  private String checkLastUpdateChanged(String lastUpdated, UUID paoId) throws Exception {
    var apiPao = mvcUtils.getPao(paoId);
    assertNotEquals(lastUpdated, apiPao.getLastUpdatedDate());
    return apiPao.getLastUpdatedDate();
  }
}
