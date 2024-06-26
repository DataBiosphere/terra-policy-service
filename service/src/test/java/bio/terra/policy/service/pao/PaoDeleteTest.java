package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.testutils.TestUnitBase;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PaoDeleteTest extends TestUnitBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String GROUP = "group";
  private static final String REGION = "region-name";
  private static final String DDGROUP = "ddgroup";
  private static final String US_REGION = "usa";

  @Autowired private PaoService paoService;

  /** A standalone PAO with no sources and no dependents should be marked as deleted in the DB. */
  @Test
  void deleteStandalonePaoMarksDeleted() {
    var objectId = UUID.randomUUID();
    createDefaultPao(objectId);

    // Retrieve and validate
    Pao pao = paoService.getPao(objectId, true);
    assertEquals(objectId, pao.getObjectId());
    assertFalse(pao.getDeleted());

    // Delete removes the PAO from the DB
    paoService.deletePao(objectId);
    final var deletedPao = paoService.getPao(objectId, true);
    assertNotNull(deletedPao);
    assertTrue(deletedPao.getDeleted());
  }

  /**
   * If the PAO being deleted is referenced as a source by another PAO, then it gets marked as
   * deleted.
   *
   * <pre>
   *     {dependentPao}
   *          |
   *     {sourcePao} <-- delete here
   * </pre>
   *
   * Result should be both PAOs are still in the graph, sourcePao is marked as deleted.
   */
  @Test
  void deleteSourcePaoMarksDeleted() {
    var sourcePaoId = UUID.randomUUID();
    var dependentPaoId = UUID.randomUUID();
    createDefaultPao(sourcePaoId);
    createDefaultPao(dependentPaoId);
    paoService.linkSourcePao(dependentPaoId, sourcePaoId, PaoUpdateMode.FAIL_ON_CONFLICT);

    // Call delete on the source PAO
    paoService.deletePao(sourcePaoId);

    // Since sourcePao has a dependent, it should be flagged as 'deleted' but should not be removed
    // from the db.
    final var sourcePao = paoService.getPao(sourcePaoId, true);
    assertNotNull(sourcePao);
    assertEquals(sourcePaoId, sourcePao.getObjectId());
    assertTrue(sourcePao.getDeleted());

    // Dependent should still link back to the source and not be deleted.
    final var dependentPao = paoService.getPao(dependentPaoId, true);
    assertNotNull(dependentPao);
    assertTrue(dependentPao.getSourceObjectIds().contains(sourcePaoId));
    assertFalse(dependentPao.getDeleted());
  }

  private void createDefaultPao(UUID objectId) {
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
  }
}
