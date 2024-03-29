package bio.terra.policy.service.pao.graph;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.service.pao.graph.model.AttributeEvaluator;
import bio.terra.policy.service.pao.graph.model.GraphAttribute;
import bio.terra.policy.service.pao.graph.model.GraphAttributeSet;
import bio.terra.policy.service.pao.graph.model.GraphNode;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * We make a Walker class each time we need to perform a graph walk. It encapsulates the entire
 * walking structure and returns the results. It calls the DAO, but has no transaction controls.
 */
public class Walker {
  private final PaoDao paoDao;
  private final Map<UUID, GraphNode> paoMap;
  private final List<PolicyConflict> newConflicts;
  /**
   * Constructing the Walker object performs the graph walk. That computes new effective policies
   * for the objects.
   *
   * @param paoDao reference to the DAO so we can read and possibly update policies
   * @param pao with proposed modification
   * @param changedPaoId object id of the change. If this is a change to the policy of an object,
   *     the id will be the same as the pao. If this is adding a new source to the object, this will
   *     be the id of the new source Pao. TODO: is this the best way to express the change?
   */
  public Walker(PaoDao paoDao, Pao pao, UUID changedPaoId) {
    this.paoDao = paoDao;
    this.paoMap = new HashMap<>();
    this.newConflicts = new ArrayList<>();

    GraphNode targetNode = new GraphNode(pao, true);
    paoMap.put(pao.getObjectId(), targetNode);
    walkNode(targetNode, changedPaoId);
    // Fill in the resulting effective attributes, so they can be returned in the update response
    targetNode.getPao().setEffectiveAttributes(targetNode.getEffectivePolicyAttributes());
  }

  /** Apply the changes computed by the walker */
  public void applyChanges() {
    // Collect all of the changed graph nodes
    List<GraphNode> changeList = new ArrayList<>();
    for (GraphNode node : paoMap.values()) {
      if (node.isModified()) {
        changeList.add(node);
      }
    }
    // Ask the DAO to update them
    paoDao.updatePaos(changeList);
  }

  /**
   * Getter for returning new conflicts from the walk
   *
   * @return list of any new policy conflicts
   */
  public List<PolicyConflict> getNewConflicts() {
    return newConflicts;
  }

  /**
   * Recursive graph walker
   *
   * @param inputNode graph node we are processing
   * @param changedPaoId the object id of the Pao that changed
   */
  private void walkNode(GraphNode inputNode, UUID changedPaoId) {
    // Build graph nodes for all of the sources
    makeSourcesList(inputNode);

    // Construct the evaluation structure for computing the effective of this node
    AttributeEvaluator evaluator = new AttributeEvaluator(inputNode.getPao());
    evaluator.addAttributeSet(inputNode.getObjectAttributeSet());
    for (GraphNode source : inputNode.getSources()) {
      evaluator.addAttributeSet(source.getEffectiveAttributeSet());
    }
    GraphAttributeSet newEffectiveAttributes = evaluator.evaluate(changedPaoId);
    List<PolicyConflict> conflicts = gatherNewConflicts(newEffectiveAttributes);

    // If there is no change to the input effective attribute set and there are no new
    // conflicts, then we stop recursing. We won't cause a change to our dependents.
    if (newEffectiveAttributes.equals(inputNode.getEffectiveAttributeSet())
        && conflicts.isEmpty()) {
      return;
    }

    // There was a change. Save the change as the new effective attributes
    // and mark the node modified so we know to write it back to the database later.
    // Save the conflicts to the walker list of all new conflicts
    inputNode.setEffectiveAttributeSet(newEffectiveAttributes);
    inputNode.setModified(true);
    newConflicts.addAll(conflicts);

    // Recursively walk our dependents. We know that these dependents will
    // refer to this changed node in recalculating their effective attribute set.
    // When we recurse, this Pao is the one that changed
    makeDependentsList(inputNode);
    for (GraphNode dependent : inputNode.getDependents()) {
      walkNode(dependent, inputNode.getPao().getObjectId());
    }
  }

  private void makeSourcesList(GraphNode node) {
    // If we already have the sources, do nothing
    if (node.getSources() != null) {
      return;
    }
    List<GraphNode> sources = makeGraphList(node.getPao().getSourceObjectIds());
    node.setSources(sources);
  }

  private void makeDependentsList(GraphNode node) {
    // If we already have the dependents, do nothing
    if (node.getDependents() != null) {
      return;
    }
    Set<UUID> dependentIds = paoDao.getDependentIds(node.getPao().getObjectId());
    List<GraphNode> dependents = makeGraphList(dependentIds);
    node.setDependents(dependents);
  }

  private List<GraphNode> makeGraphList(Set<UUID> idList) {
    List<GraphNode> graphList = new ArrayList<>();
    List<UUID> daoFetchIds = new ArrayList<>();
    // Check the map; if the node is found in our map, add it to the result list.
    // If not, add it to the to-be-fetched from the database list.
    for (UUID id : idList) {
      GraphNode foundNode = paoMap.get(id);
      if (foundNode == null) {
        daoFetchIds.add(id);
      } else {
        graphList.add(foundNode);
      }
    }

    // Fetch the Paos that were not in the map; make graph nodes and add them to the map.
    // New nodes always start without new conflicts and unmodified.
    List<Pao> daoFetchResult = paoDao.getPaos(daoFetchIds);
    for (Pao pao : daoFetchResult) {
      var node = new GraphNode(pao, false);
      graphList.add(node);
      paoMap.put(pao.getObjectId(), node);
    }
    return graphList;
  }

  private List<PolicyConflict> gatherNewConflicts(GraphAttributeSet attributeSet) {
    List<PolicyConflict> conflicts = new ArrayList<>();
    for (GraphAttribute graphAttribute : attributeSet.getAttributes()) {
      Pao containingPao = graphAttribute.getContainingPao();

      // Build a conflict result for each new conflict
      for (UUID conflictId : graphAttribute.getNewConflicts()) {
        Pao conflictPao = getPaoFromGraphNode(conflictId);
        conflicts.add(
            new PolicyConflict(
                containingPao, conflictPao, graphAttribute.getPolicyInput().getPolicyName()));
      }
    }
    return conflicts;
  }

  private Pao getPaoFromGraphNode(UUID objectId) {
    GraphNode node = paoMap.get(objectId);
    if (node == null) {
      throw new InternalTpsErrorException("Unexpected null graph node!");
    }
    return node.getPao();
  }
}
