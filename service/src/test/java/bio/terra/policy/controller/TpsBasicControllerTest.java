package bio.terra.policy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  private static final String US_REGION = "US";

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

    var inputs = new ApiTpsPolicyInputs().addInputsItem(groupPolicy).addInputsItem(regionPolicy);

    // Create a PAO
    UUID paoIdA = mvcUtils.createPao(inputs);

    // Create another PAO with no policies
    UUID paoIdB = mvcUtils.createPao(new ApiTpsPolicyInputs());

    // Get a PAO
    var apiPao = mvcUtils.getPao(paoIdA);
    checkAttributeSet(apiPao.getAttributes());
    checkAttributeSet(apiPao.getEffectiveAttributes());

    // Merge a PAO
    var updateResult = mvcUtils.mergePao(paoIdB, paoIdA);
    assertTrue(updateResult.isUpdateApplied());
    assertEquals(0, updateResult.getConflicts().size());
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes());

    // Link a PAO
    updateResult = mvcUtils.linkPao(paoIdB, paoIdA);
    assertTrue(updateResult.isUpdateApplied());
    assertEquals(0, updateResult.getConflicts().size());
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes());

    // Update a PAO
    updateResult = mvcUtils.updatePao(paoIdB, inputs);
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes());

    // Replace a PAO
    updateResult = mvcUtils.replacePao(paoIdB, inputs);
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes());

    // Delete a PAO
    mvcUtils.deletePao(paoIdA);
    mvcUtils.deletePao(paoIdB);
  }

  private void checkAttributeSet(ApiTpsPolicyInputs attributeSet) {
    for (ApiTpsPolicyInput attribute : attributeSet.getInputs()) {
      assertEquals(TERRA, attribute.getNamespace());
      assertEquals(1, attribute.getAdditionalData().size());

      if (attribute.getName().equals(GROUP_CONSTRAINT)) {
        assertEquals(GROUP, attribute.getAdditionalData().get(0).getKey());
        assertEquals(DDGROUP, attribute.getAdditionalData().get(0).getValue());
      } else if (attribute.getName().equals(REGION_CONSTRAINT)) {
        assertEquals(REGION, attribute.getAdditionalData().get(0).getKey());
        assertEquals(US_REGION, attribute.getAdditionalData().get(0).getValue());
      } else {
        fail();
      }
    }
  }
}
