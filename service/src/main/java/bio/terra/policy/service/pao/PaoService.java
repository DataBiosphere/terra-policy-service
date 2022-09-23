package bio.terra.policy.service.pao;

import bio.terra.policy.common.exception.DirectConflictException;
import bio.terra.policy.common.exception.IllegalCycleException;
import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.exception.PolicyNotImplementedException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.db.DbPao;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.service.pao.graph.DeleteWalker;
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
import java.util.Set;
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
    throw new PolicyNotImplementedException("Deprecated method. Here until we change the WSM call");
  }

  /**
   * Create a policy attribute object
   *
   * @param objectId UUID of the object - client-relevant
   * @param component identity of the component, so we know what component owns UUID
   * @param objectType type of object in the component, so the component knows where to look up the
   *     UUID
   * @param inputs policy attributes
   */
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
    paoDao.markPaoDeleted(objectId);
    DeleteWalker walker = new DeleteWalker(paoDao, objectId);
    Set<DbPao> toRemove = walker.findRemovablePaos();
    paoDao.deletePaos(toRemove);
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
    logger.info(
        "LinkSourcePao: dependent {} source {} mode {}", objectId, sourceObjectId, updateMode);

    Pao targetPao = paoDao.getPao(objectId);
    boolean newSource = targetPao.getSourceObjectIds().add(sourceObjectId);

    // We didn't actually change the source list, so we are done
    if (!newSource) {
      return new PolicyUpdateResult(targetPao, new ArrayList<>(), true);
    }

    // Make sure adding this link to the target will not create a cycle;
    // that is, source cannot be one of our descendants.
    Set<UUID> allDescendents = paoDao.getAllDependentIds(objectId);
    if (allDescendents.contains(sourceObjectId)) {
      throw new IllegalCycleException(
          String.format(
              "Linking object %s to object %s would create a cycle, so is not allowed",
              sourceObjectId, objectId));
    }

    // Evaluate the change, calculating new effective attribute sets and finding conflicts
    Walker walker = new Walker(paoDao, targetPao, sourceObjectId);
    List<PolicyConflict> conflicts = walker.getNewConflicts();

    // If the mode is FAIL_ON_CONFLICT and there are no conflicts, apply the changes
    boolean updateApplied = (updateMode == PaoUpdateMode.FAIL_ON_CONFLICT && conflicts.isEmpty());
    if (updateApplied) {
      walker.applyChanges();
    }

    return new PolicyUpdateResult(targetPao, conflicts, updateApplied);
  }

  /**
   * Merge policies from one PAO into another PAO. Sometimes, like a workspace clone, we want to
   * take source policies and apply them to a destination.
   *
   * @param sourceObjectId source PAO of the policies
   * @param destinationObjectId PAO we should merge them into
   * @param updateMode DRY_RUN or FAIL_ON_CONFLICT
   * @return result of the merge - destination PAO and any policy conflicts
   */
  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
  public PolicyUpdateResult mergeFromPao(
      UUID sourceObjectId, UUID destinationObjectId, PaoUpdateMode updateMode) {
    if (updateMode == PaoUpdateMode.ENFORCE_CONFLICTS) {
      throw new InternalTpsErrorException("ENFORCE_CONFLICTS is not allowed on listSourcePao");
    }
    logger.info(
        "Merge from PAO id {} to {} mode {}", sourceObjectId, destinationObjectId, updateMode);

    // Step 0: get the paos. This will throw if they are not present
    Pao sourcePao = paoDao.getPao(sourceObjectId);
    Pao destinationPao = paoDao.getPao(destinationObjectId);

    // If the source and destination are the same PAO, there is nothing to do
    if (sourceObjectId.equals(destinationObjectId)) {
      return new PolicyUpdateResult(destinationPao, new ArrayList<>(), true);
    }

    // Step 1: combine the source attributes and destination attributes;
    //  stop here if there are conflicts
    List<PolicyConflict> conflicts = mergeAttributes(sourcePao, destinationPao);
    if (!conflicts.isEmpty()) {
      return new PolicyUpdateResult(destinationPao, conflicts, false);
    }

    // Step 2: merge the sourceObject sources into the destination sources
    destinationPao.getSourceObjectIds().addAll(sourcePao.getSourceObjectIds());

    // Step 3: do the walk computing the new effective attributes for the destination
    Walker walker = new Walker(paoDao, destinationPao, destinationObjectId);
    conflicts = walker.getNewConflicts();

    // If the mode is FAIL_ON_CONFLICT and there are no conflicts, apply the changes
    boolean updateApplied = (updateMode == PaoUpdateMode.FAIL_ON_CONFLICT && conflicts.isEmpty());
    if (updateApplied) {
      walker.applyChanges();
    }

    return new PolicyUpdateResult(destinationPao, conflicts, updateApplied);
  }

  /**
   * Update the attributes of a Pao and propagate changes.
   *
   * @param targetPaoId the object to update
   * @param replacementAttributes policy inputs to overwrite
   * @param updateMode how to handle applying the changes
   */
  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
  public PolicyUpdateResult replacePao(
      UUID targetPaoId, PolicyInputs replacementAttributes, PaoUpdateMode updateMode) {
    logger.info(
        "ReplacePao: target {} attributes {} updateMode {}",
        targetPaoId,
        replacementAttributes,
        updateMode);

    Pao targetPao = paoDao.getPao(targetPaoId);
    return updateAttributesWorker(replacementAttributes, targetPao, updateMode);
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

    // Now integrate the adds into the attribute set
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

    return updateAttributesWorker(newAttributes, targetPao, updateMode);
  }

  // Common code to update new attributes to a targetPao
  // Used by both updatePao and replacePao
  private PolicyUpdateResult updateAttributesWorker(
      PolicyInputs newAttributes, Pao targetPao, PaoUpdateMode updateMode) {
    // Set the target PAO attributes to the newly computed attributes
    targetPao.setAttributes(newAttributes);

    // Evaluate the change, calculating new effective attribute sets and finding conflicts
    Walker walker = new Walker(paoDao, targetPao, targetPao.getObjectId());
    List<PolicyConflict> conflicts = walker.getNewConflicts();

    if (updateMode == PaoUpdateMode.DRY_RUN
        || updateMode == PaoUpdateMode.FAIL_ON_CONFLICT && !conflicts.isEmpty()) {
      return new PolicyUpdateResult(targetPao, conflicts, false);
    }

    if (updateMode == PaoUpdateMode.ENFORCE_CONFLICTS && !conflicts.isEmpty()) {
      // We disallow enforcing direct conflicts; that is, this update should not have
      // created any conflicts on the target Pao. We only allow conflicts on dependent
      // Paos.
      for (PolicyConflict conflict : conflicts) {
        if (conflict.pao().getObjectId().equals(targetPao.getObjectId())) {
          throw new DirectConflictException(
              String.format(
                  "Update of policy %s on %s creates a conflict",
                  conflict.policyName().getKey(), targetPao.toShortString()));
        }
      }
    }

    walker.applyChanges();
    return new PolicyUpdateResult(targetPao, conflicts, true);
  }

  /**
   * This method does the first step of clone: merging the source attributes into the destination
   * attributes. There are never conflicts on the attribute set of objects - only on the effective
   * attributes. So this merge is much simpler than what we do in the general walking case.
   *
   * <p>We return any conflicts found in the process.
   *
   * @param sourcePao source of the clone
   * @param destinationPao destination of the clone
   * @return conflict list
   */
  private List<PolicyConflict> mergeAttributes(Pao sourcePao, Pao destinationPao) {
    List<PolicyConflict> conflicts = new ArrayList<>();
    PolicyInputs policyInputs = destinationPao.getAttributes();

    for (PolicyInput input : sourcePao.getAttributes().getInputs().values()) {
      PolicyInput destinationMatchedPolicy = policyInputs.lookupPolicy(input);
      // If the policy does not exist in the destination, so we add it
      if (destinationMatchedPolicy == null) {
        policyInputs.addInput(input);
      } else {
        PolicyInput resultInput = PolicyMutator.combine(destinationMatchedPolicy, input);
        if (resultInput == null) {
          // Uh oh, we hit a conflict
          conflicts.add(new PolicyConflict(destinationPao, sourcePao, input.getPolicyName()));
        } else {
          // Replace the policy with the combined policy
          policyInputs.addInput(resultInput);
        }
      }
    }

    return conflicts;
  }
}
