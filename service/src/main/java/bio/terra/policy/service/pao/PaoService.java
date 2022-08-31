package bio.terra.policy.service.pao;

import bio.terra.policy.common.exception.DirectConflictException;
import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.service.pao.graph.Walker;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.service.policy.PolicyMutator;
import bio.terra.policy.service.policy.model.PolicyUpdateResult;
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

  public void clonePao(UUID sourceObjectId, UUID destinationObjectId) {
    logger.info("Clone PAO id {} to {}", sourceObjectId, destinationObjectId);
    paoDao.clonePao(sourceObjectId, destinationObjectId);
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
   * @param objectId id of the target (dependent) object
   * @param sourceObjectId id of the source object
   * @param updateMode link mode: fail on conflict or dry_run
   */
  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
  public PolicyUpdateResult linkSourcePao(
      UUID objectId, UUID sourceObjectId, PaoUpdateMode updateMode) {
    if (updateMode == PaoUpdateMode.ENFORCE_CONFLICTS) {
      throw new InternalTpsErrorException("ENFORCE_CONFLICTS is not allowed on listSourcePao");
    }
    logger.info("LinkSourcePao: dependent {} source {}", objectId, sourceObjectId);

    Pao targetPao = paoDao.getPao(objectId);
    boolean newSource = targetPao.getSourceObjectIds().add(sourceObjectId);

    // We didn't actually change the source list, so we are done
    if (!newSource) {
      return new PolicyUpdateResult(targetPao, new ArrayList<>());
    }

    // Evaluate the change, calculating new effective attribute sets and finding conflicts
    Walker walker = new Walker(paoDao, targetPao, sourceObjectId);
    List<PolicyConflict> conflicts = walker.getNewConflicts();

    // If the mode is FAIL_ON_CONFLICT and there are no conflicts, apply the changes
    if (updateMode == PaoUpdateMode.FAIL_ON_CONFLICT && conflicts.isEmpty()) {
      walker.applyChanges();
    }

    return new PolicyUpdateResult(targetPao, conflicts);
  }

  /**
   * Update the attributes of a Pao and propagate changes.
   *
   * @param targetPaoId the object to update
   * @param addAttributes policy inputs to add
   * @param removeAttributes policy inputs to remove
   * @param updateMode how to handle applying the changes
   */
  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
  public PolicyUpdateResult updatePao(
      UUID targetPaoId,
      PolicyInputs addAttributes,
      PolicyInputs removeAttributes,
      PaoUpdateMode updateMode) {
    logger.info(
        "UpdatePao: target {} adds {} removes {} updateMode {}",
        targetPaoId,
        addAttributes,
        removeAttributes,
        updateMode);

    Pao targetPao = paoDao.getPao(targetPaoId);
    PolicyInputs newAttributes = new PolicyInputs();

    // We do the removes first, so we don't remove newly added things
    for (PolicyInput removePolicy : removeAttributes.getInputs().values()) {
      PolicyInput existingPolicy = targetPao.getAttributes().lookupPolicy(removePolicy);
      if (existingPolicy != null) {
        // We have something to remove
        PolicyInput removeResult = PolicyMutator.remove(existingPolicy, removePolicy);
        if (removeResult != null) {
          // There is something left of the policy to keep
          newAttributes.addInput(removeResult);
        }
      }
    }

    // Now the adds
    for (PolicyInput addPolicy : addAttributes.getInputs().values()) {
      PolicyInput existingPolicy = targetPao.getAttributes().lookupPolicy(addPolicy);
      if (existingPolicy == null) {
        // Nothing to combine; just take the new
        newAttributes.addInput(addPolicy);
      } else {
        PolicyInput addResult = PolicyMutator.combine(existingPolicy, addPolicy);
        if (addResult != null) {
          // We have a combined policy to add
          newAttributes.addInput(addResult);
        } else {
          throw new DirectConflictException(
              String.format(
                  "Update of policy %s adding %s creates a conflict",
                  existingPolicy.getKey(), addPolicy.getKey()));
        }
      }
    }

    // Set the attributes to the newly computed attributes
    targetPao.setAttributes(newAttributes);

    // Evaluate the change, calculating new effective attribute sets and finding conflicts
    Walker walker = new Walker(paoDao, targetPao, targetPao.getObjectId());
    List<PolicyConflict> conflicts = walker.getNewConflicts();

    if (updateMode == PaoUpdateMode.DRY_RUN
        || updateMode == PaoUpdateMode.FAIL_ON_CONFLICT && !conflicts.isEmpty()) {
      return new PolicyUpdateResult(targetPao, conflicts);
    }

    if (updateMode == PaoUpdateMode.ENFORCE_CONFLICTS && !conflicts.isEmpty()) {
      // We disallow enforcing direct conflicts; that is, this update should not have
      // created any conflicts on the target Pao. We only allow conflicts on dependent
      // Paos.
      for (PolicyConflict conflict : conflicts) {
        if (conflict.pao().getObjectId().equals(targetPaoId)) {
          throw new DirectConflictException(
              String.format(
                  "Update of policy %s on %s creates a conflict",
                  conflict.policyName().getKey(), targetPao.toShortString()));
        }
      }
    }

    walker.applyChanges();
    return new PolicyUpdateResult(targetPao, conflicts);
  }
}
