package bio.terra.policy.service.pao;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.policy.common.exception.PolicyObjectNotFoundException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.testutils.LibraryTestBase;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PaoDeleteTest extends LibraryTestBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String GROUP = "group";
  private static final String REGION = "region";
  private static final String DDGROUP = "ddgroup";
  private static final String US_REGION = "US";

  @Autowired private PaoService paoService;

  /**
   * A standalone PAO with no sources and no dependents should be removed from the DB.
   *
   * @throws Exception
   */
  @Test
  void deletePaoRemovesFromDb() throws Exception {
    var objectId = UUID.randomUUID();
    createDefaultPao(objectId);

    // Retrieve and validate
    Pao pao = paoService.getPao(objectId);
    assertEquals(objectId, pao.getObjectId());

    // Delete removes the PAO from the DB
    paoService.deletePao(objectId);
    assertThrows(PolicyObjectNotFoundException.class, () -> paoService.getPao(objectId));
  }

  /**
   * If the PAO being deleted is referenced as a source by another PAO, then it doesn't get removed
   * from the graph but gets marked as deleted.
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
  void deletePaoMarksDeleted() {
    var sourcePaoId = UUID.randomUUID();
    var dependentPaoId = UUID.randomUUID();
    createDefaultPao(sourcePaoId);
    createDefaultPao(dependentPaoId);
    paoService.linkSourcePao(dependentPaoId, sourcePaoId, PaoUpdateMode.FAIL_ON_CONFLICT);

    // Call delete on the source PAO
    paoService.deletePao(sourcePaoId);

    // Since sourcePao has a dependent, it should be flagged as 'deleted' but should not be removed
    // from the db.
    final var sourcePao = paoService.getPao(sourcePaoId);
    assertNotNull(sourcePao);
    assertEquals(sourcePaoId, sourcePao.getObjectId());
    assertTrue(sourcePao.getDeleted());

    // Dependent should still link back to the source and not be deleted.
    final var dependentPao = paoService.getPao(dependentPaoId);
    assertNotNull(dependentPao);
    assertTrue(dependentPao.getSourceObjectIds().contains(sourcePaoId));
    assertFalse(dependentPao.getDeleted());
  }

  /**
   * In this case, the deleted PAO has two sources, one of which was previously flagged as deleted.
   *
   * <pre>
   *     {targetPao} <-- delete second
   *      /         \
   * {sourcePao1}  {sourcePao2*} <-- delete first
   *                  \
   *                {sourcePao3}
   * </pre>
   *
   * The result should be that targetPao and sourcePao2 should be removed from the db. The other
   * PAOs should remain.
   */
  @Test
  void deletePaoRemovesPreviouslyFlaggedSource() throws Exception {
    var targetObjectId = UUID.randomUUID();
    var source1ObjectId = UUID.randomUUID();
    var source2ObjectId = UUID.randomUUID();
    var source3ObjectId = UUID.randomUUID();
    createDefaultPao(targetObjectId);
    createDefaultPao(source1ObjectId);
    createDefaultPao(source2ObjectId);
    createDefaultPao(source3ObjectId);

    paoService.linkSourcePao(targetObjectId, source1ObjectId, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(targetObjectId, source2ObjectId, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(targetObjectId, source3ObjectId, PaoUpdateMode.FAIL_ON_CONFLICT);

    // Call delete on sourcePao2
    paoService.deletePao(source2ObjectId);
    var pao = paoService.getPao(source2ObjectId);
    assertNotNull(pao);
    assertTrue(pao.getDeleted());

    // Call delete on targetPao
    paoService.deletePao(targetObjectId);

    // targetPao and sourcePao2 should be deleted
    assertThrows(PolicyObjectNotFoundException.class, () -> paoService.getPao(targetObjectId));
    assertThrows(PolicyObjectNotFoundException.class, () -> paoService.getPao(source2ObjectId));

    // sourcePao1 and sourcePao3 should not be
    assertNotNull(paoService.getPao(source1ObjectId));
    assertNotNull(paoService.getPao(source3ObjectId));
  }

  /**
   * Recursive test: deleting a Pao should visit all source Paos recursively and apply the delete
   * logic on them.
   *
   * <pre>
   *                    {targetPao#} <-- delete last
   *                    /       \
   * delete 2nd--> {pao2#}     {pao3}
   *                /   \
   *            {pao4}   \    {pao6}
   *                      \   /
   *                    {pao5*}  <-- delete 1st
   *                       |
   *                    {pao7}
   * </pre>
   *
   * The result should be that the PAO marked with a * should be flagged as deleted. All PAOs marked
   * with a # should be removed from the DB
   */
  @Test
  void deleteRecursive() throws Exception {
    var targetObjectId = UUID.randomUUID();
    var pao2Id = UUID.randomUUID();
    var pao3Id = UUID.randomUUID();
    var pao4Id = UUID.randomUUID();
    var pao5Id = UUID.randomUUID();
    var pao6Id = UUID.randomUUID();
    var pao7Id = UUID.randomUUID();
    createDefaultPao(targetObjectId);
    createDefaultPao(pao2Id);
    createDefaultPao(pao3Id);
    createDefaultPao(pao4Id);
    createDefaultPao(pao5Id);
    createDefaultPao(pao6Id);
    createDefaultPao(pao7Id);

    paoService.linkSourcePao(targetObjectId, pao2Id, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(targetObjectId, pao3Id, PaoUpdateMode.FAIL_ON_CONFLICT);

    paoService.linkSourcePao(pao2Id, pao4Id, PaoUpdateMode.FAIL_ON_CONFLICT);
    paoService.linkSourcePao(pao2Id, pao5Id, PaoUpdateMode.FAIL_ON_CONFLICT);

    paoService.linkSourcePao(pao6Id, pao5Id, PaoUpdateMode.FAIL_ON_CONFLICT);

    paoService.linkSourcePao(pao5Id, pao7Id, PaoUpdateMode.FAIL_ON_CONFLICT);

    // Call first delete to mark Pao5 deleted.
    paoService.deletePao(pao5Id);
    var pao = paoService.getPao(pao5Id);
    assertTrue(pao.getDeleted());

    // call second delete to mark Pao2 deleted
    paoService.deletePao(pao2Id);
    pao = paoService.getPao(pao2Id);
    assertTrue(pao.getDeleted());

    // call final delete to remove targetPao
    paoService.deletePao(targetObjectId);

    // These PAOs should have been removed from the DB
    assertThrows(PolicyObjectNotFoundException.class, () -> paoService.getPao(targetObjectId));
    assertThrows(PolicyObjectNotFoundException.class, () -> paoService.getPao(pao2Id));

    // This PAO should be marked as deleted but not removed from the db
    final var pao5 = paoService.getPao(pao5Id);
    assertNotNull(pao5);
    assertTrue(pao5.getDeleted());

    // These PAOs should still exist
    assertNotNull(paoService.getPao(pao3Id));
    assertNotNull(paoService.getPao(pao4Id));
    assertNotNull(paoService.getPao(pao6Id));
    assertNotNull(paoService.getPao(pao7Id));
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
