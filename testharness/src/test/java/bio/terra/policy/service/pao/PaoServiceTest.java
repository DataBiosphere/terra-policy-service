package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.exception.PolicyObjectNotFoundException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.testutils.LibraryTestBase;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PaoServiceTest extends LibraryTestBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String GROUP = "group";
  private static final String REGION = "region";
  private static final String DDGROUP = "ddgroup";
  private static final String US_REGION = "US";

  @Autowired private PaoService paoService;

  @Test
  void createPaoTest() throws Exception {
    var objectId = UUID.randomUUID();

    var groupPolicy =
        PolicyInput.createFromMap(
            TERRA, GROUP_CONSTRAINT, Collections.singletonMap(GROUP, DDGROUP));
    var regionPolicy =
        PolicyInput.createFromMap(
            TERRA, REGION_CONSTRAINT, Collections.singletonMap(REGION, US_REGION));

    var inputs = new PolicyInputs();
    inputs.addInput(groupPolicy);
    inputs.addInput(regionPolicy);

    // Create a PAO
    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, inputs);

    // Retrieve and validate
    Pao pao = paoService.getPao(objectId);
    assertEquals(objectId, pao.getObjectId());
    assertEquals(PaoComponent.WSM, pao.getComponent());
    assertEquals(PaoObjectType.WORKSPACE, pao.getObjectType());
    checkAttributeSet(pao.getAttributes(), groupPolicy, regionPolicy);
    checkAttributeSet(pao.getEffectiveAttributes(), groupPolicy, regionPolicy);

    // Delete
    paoService.deletePao(objectId);

    assertThrows(PolicyObjectNotFoundException.class, () -> paoService.getPao(objectId));
  }

  private void checkAttributeSet(
      PolicyInputs attributeSet, PolicyInput groupPolicy, PolicyInput regionPolicy) {
    PolicyInput groupConstraint = attributeSet.getInputs().get(groupPolicy.getKey());
    assertNotNull(groupConstraint);
    assertEquals(TERRA, groupConstraint.getPolicyName().getNamespace());
    assertEquals(GROUP_CONSTRAINT, groupConstraint.getPolicyName().getName());
    Collection<String> dataValue = groupConstraint.getAdditionalData().get(GROUP);
    assertNotNull(dataValue);
    assertTrue(dataValue.contains(DDGROUP));

    PolicyInput regionConstraint = attributeSet.getInputs().get(regionPolicy.getKey());
    assertNotNull(regionConstraint);
    assertEquals(TERRA, regionConstraint.getPolicyName().getNamespace());
    assertEquals(REGION_CONSTRAINT, regionConstraint.getPolicyName().getName());
    dataValue = regionConstraint.getAdditionalData().get(REGION);
    assertNotNull(dataValue);
    assertTrue(dataValue.contains(US_REGION));
  }
}
