package bio.terra.policy.service.pao;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.service.pao.graph.Walker;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.service.policy.model.LinkSourceResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaoService {
  private static final Logger logger = LoggerFactory.getLogger(PaoService.class);

  private final PaoDao paoDao;

  @Autowired
  public PaoService(PaoDao paoDao) {
    this.paoDao = paoDao;
  }

  public void createPao(
      UUID objectId, PaoComponent component, PaoObjectType objectType, PolicyInputs inputs) {
    logger.info(
        "Create PAO id {} component {} object type {}",
        objectId,
        component.name(),
        objectType.name());

    // TODO: Validate policy inputs against the policy descriptions when those are available.

    // The DAO does the heavy lifting.
    paoDao.createPao(objectId, component, objectType, inputs);
  }

  public void deletePao(UUID objectId) {
    logger.info("Delete PAO id {}", objectId);
    paoDao.deletePao(objectId);
  }

  public Pao getPao(UUID objectId) {
    logger.info("Get PAO id {}", objectId);
    return paoDao.getPao(objectId);
  }

  /**
   * Link a policy source to a PAO. For example, referencing a data collection in a workspace would
   * add the data collection as a policy source of the workspace.
   *
   * @param objectId id of the parent object
   * @param sourceObjectId id of the source object
   * @param updateMode link mode: fail on conflict or dry_run
   */
  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
  public LinkSourceResult linkSourcePao(
      UUID objectId, UUID sourceObjectId, PaoUpdateMode updateMode) {
    if (updateMode == PaoUpdateMode.ENFORCE_CONFLICTS) {
      throw new InternalTpsErrorException("ENFORCE_CONFLICTS is not allowed on listSourcePao");
    }

    // Build a copy of the Pao with the desired change
    // We duplicate the target Pao, but we do not copy the conflict annotations.
    // That way we can detect new conflicts and when conflicts are resolved.
    Pao targetPao = paoDao.getPao(objectId);
    Pao modifiedPao = targetPao.duplicateWithoutConflicts();
    modifiedPao.getSourceObjectIds().add(sourceObjectId);

    // If nothing has actually changed, we are done
    if (modifiedPao.getSourceObjectIds().equals(targetPao.getSourceObjectIds())) {
      return new LinkSourceResult(targetPao, new ArrayList<>());
    }

    // Evaluate the change, calculating new effective attribute sets and finding conflicts
    Walker walker = new Walker(paoDao, targetPao, modifiedPao);
    List<PolicyConflict> conflicts = walker.walk();

    // If the mode is FAIL_ON_CONFLICT and there are no conflicts, apply the changes
    if (updateMode == PaoUpdateMode.FAIL_ON_CONFLICT && conflicts.isEmpty()) {
      walker.applyChanges();
    }

    return new LinkSourceResult(modifiedPao, conflicts);
  }
}
