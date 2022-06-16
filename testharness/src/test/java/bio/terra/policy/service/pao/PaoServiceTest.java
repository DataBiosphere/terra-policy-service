package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.policy.common.exception.PolicyObjectNotFoundException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.testutils.LibraryTestBase;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
        new PolicyInput(TERRA, GROUP_CONSTRAINT, Collections.singletonMap(GROUP, DDGROUP));
    var regionPolicy =
        new PolicyInput(TERRA, REGION_CONSTRAINT, Collections.singletonMap(REGION, US_REGION));

    Map<String, PolicyInput> policyMap = new HashMap<>();
    policyMap.put(
        PolicyInputs.composeKey(groupPolicy.getNamespace(), groupPolicy.getName()), groupPolicy);
    policyMap.put(
        PolicyInputs.composeKey(regionPolicy.getNamespace(), regionPolicy.getName()), regionPolicy);

    var inputs = new PolicyInputs(policyMap);

    // Create a PAO
    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, inputs);

    // Retrieve and validate
    Pao pao = paoService.getPao(objectId);
    assertEquals(objectId, pao.getObjectId());
    assertEquals(PaoComponent.WSM, pao.getComponent());
    assertEquals(PaoObjectType.WORKSPACE, pao.getObjectType());
    checkAttributeSet(pao.getAttributes());
    checkAttributeSet(pao.getEffectiveAttributes());

    // Delete
    paoService.deletePao(objectId);

    assertThrows(PolicyObjectNotFoundException.class, () -> paoService.getPao(objectId));
  }

  private void checkAttributeSet(PolicyInputs attributeSet) {
    String groupKey = PolicyInputs.composeKey(TERRA, GROUP_CONSTRAINT);
    PolicyInput groupConstraint = attributeSet.getInputs().get(groupKey);
    assertNotNull(groupConstraint);
    assertEquals(TERRA, groupConstraint.getNamespace());
    assertEquals(GROUP_CONSTRAINT, groupConstraint.getName());
    String dataValue = groupConstraint.getAdditionalData().get(GROUP);
    assertNotNull(dataValue);
    assertEquals(DDGROUP, dataValue);

    String regionKey = PolicyInputs.composeKey(TERRA, REGION_CONSTRAINT);
    PolicyInput regionConstraint = attributeSet.getInputs().get(regionKey);
    assertNotNull(regionConstraint);
    assertEquals(TERRA, regionConstraint.getNamespace());
    assertEquals(REGION_CONSTRAINT, regionConstraint.getName());
    dataValue = regionConstraint.getAdditionalData().get(REGION);
    assertNotNull(dataValue);
    assertEquals(US_REGION, dataValue);
  }
}
