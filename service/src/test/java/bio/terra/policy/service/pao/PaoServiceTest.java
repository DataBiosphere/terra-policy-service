package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.common.exception.PolicyObjectNotFoundException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PaoServiceTest extends TestUnitBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String GROUP = "group";
  private static final String REGION = "region-name";
  private static final String DDGROUP = "ddgroup";
  private static final String US_REGION = "usa";

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

  @Test
  void listPaosTest() {
    var objectId = UUID.randomUUID();
    var objectId2 = UUID.randomUUID();

    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, new PolicyInputs());
    paoService.createPao(objectId2, PaoComponent.WSM, PaoObjectType.WORKSPACE, new PolicyInputs());

    // add an extra object id that doesn't exist
    var paos = paoService.listPaos(List.of(objectId, objectId2, UUID.randomUUID()));
    assertEquals(2, paos.size());
    assertTrue(paos.stream().anyMatch(p -> p.getObjectId().equals(objectId)));
    assertTrue(paos.stream().anyMatch(p -> p.getObjectId().equals(objectId2)));
  }

  @Test
  void createPaoTest_invalidInputThrows() throws Exception {
    var objectId = UUID.randomUUID();

    var invalidGroupPolicy =
        PolicyInput.createFromMap(
            TERRA, GROUP_CONSTRAINT, Collections.singletonMap("badkey", DDGROUP));
    var invalidRegionPolicy =
        PolicyInput.createFromMap(
            TERRA, REGION_CONSTRAINT, Collections.singletonMap(REGION, "badregion"));

    var groupInputs = new PolicyInputs();
    groupInputs.addInput(invalidGroupPolicy);
    assertThrows(
        InvalidInputException.class,
        () ->
            paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, groupInputs));

    var regionInputs = new PolicyInputs();
    regionInputs.addInput(invalidRegionPolicy);
    assertThrows(
        InvalidInputException.class,
        () ->
            paoService.createPao(
                objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, regionInputs));
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
