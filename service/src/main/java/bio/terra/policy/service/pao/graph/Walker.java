package bio.terra.policy.service.pao.graph;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.service.pao.graph.model.GraphNode;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.policy.PolicyCombiner;
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
  private final GraphNode targetNode;

  public Walker(PaoDao paoDao, Pao initialPao, Pao modifiedPao) {
    this.paoDao = paoDao;
    this.paoMap = new HashMap<>();
    this.targetNode =
        new GraphNode()
            .setInitialPao(initialPao)
            .setComputePao(modifiedPao)
            .setModified(true)
            .setNewConflict(false);
    paoMap.put(modifiedPao.getObjectId(), targetNode);
  }

  public void applyChanges() {
    List<GraphNode> changeList = new ArrayList<>();
    for (GraphNode node : paoMap.values()) {
      // If we hit a conflict and there wasn't one before, or we modified the effective
      // attribute set, then we need to update the Pao.
      if (node.isNewConflict() || node.isModified()) {
        changeList.add(node);
      }
    }
    paoDao.updatePaos(changeList);
  }

  /**
   * Recursively walk the policy graph building a map of modified Paos and a list of the conflicts
   * caused by the modified policy.
   *
   * @return list of policy conflicts
   */
  public List<PolicyConflict> walk() {
    walkNode(targetNode);
    return computeConflict();
  }

  private void walkNode(GraphNode inputNode) {
    // We re-compute the effective attribute set from our set attributes and our sources
    makeSourcesList(inputNode);
    Pao inputPao = inputNode.getComputePao();
    PolicyInputs runningEffectiveSet = inputPao.getAttributes();
    for (GraphNode source : inputNode.getSources()) {
      runningEffectiveSet = computeEffective(runningEffectiveSet, source);
    }

    // If there is no change to the input effective attribute set, then we stop recursing.
    // We will not cause a change in any of our dependents. We still might have a conflict.
    if (runningEffectiveSet.equals(inputPao.getEffectiveAttributes())) {
      return;
    }

    // There was a change, so we have more work to do.
    // Save the changes and mark us as changed.
    inputPao.setEffectiveAttributes(runningEffectiveSet);
    inputNode.setModified(true);

    // Recursively walk our dependents. We know that these dependents will
    // refer to this changed node in recalculating their effective attribute set.
    makeDependentsList(inputNode);
    for (GraphNode dependent : inputNode.getDependents()) {
      walkNode(dependent);
    }
  }

  private PolicyInputs computeEffective(PolicyInputs dependentInputs, GraphNode sourceNode) {
    PolicyInputs newDependentEffective = new PolicyInputs();
    PolicyInputs sourceInputs = sourceNode.getComputePao().getEffectiveAttributes();

    // First traverse the input and probe the source. We take care of all combining
    // in this pass.
    for (PolicyInput dependentInput : dependentInputs.getInputs().values()) {
      PolicyInput sourceInput = sourceInputs.lookupPolicy(dependentInput);
      if (sourceInput == null) {
        newDependentEffective.addInput(dependentInput);
      } else {
        PolicyInput resultInput = PolicyCombiner.combine(dependentInput, sourceInput);
        if (resultInput == null) {
          // Combiner failed, so we have a conflict. Record the conflict in the input's
          // conflict set. Use the existing dependent input as the effective policy; that is,
          // when there is a conflict, we retain the existing policy setting and remember
          // the conflict.
          dependentInput.getConflicts().add(sourceNode.getComputePao().getObjectId());
          newDependentEffective.addInput(dependentInput);
        } else {
          newDependentEffective.addInput(resultInput);
        }
      }
    }

    // Second traverse the source and probe the dependents. Add any non-matches and ignore the rest
    for (PolicyInput input : sourceInputs.getInputs().values()) {
      PolicyInput dependentInput = dependentInputs.lookupPolicy(input);
      if (dependentInput == null) {
        newDependentEffective.addInput(input);
      }
    }

    return newDependentEffective;
  }

  private void makeSourcesList(GraphNode node) {
    // If we already have the sources, do nothing
    if (node.getSources() != null) {
      return;
    }
    List<GraphNode> sources = makeGraphList(node.getComputePao().getSourceObjectIds());
    node.setSources(sources);
  }

  private void makeDependentsList(GraphNode node) {
    // If we already have the dependents, do nothing
    if (node.getDependents() != null) {
      return;
    }
    Set<UUID> dependentIds = paoDao.getDependentIds(node.getComputePao().getObjectId());
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
    // New nodes always start without conflicts and unmodified.
    List<Pao> daoFetchResult = paoDao.getPaos(daoFetchIds);
    for (Pao pao : daoFetchResult) {
      var node =
          new GraphNode()
              .setInitialPao(pao)
              .setComputePao(pao.duplicateWithoutConflicts())
              .setModified(false)
              .setNewConflict(false);
      graphList.add(node);
      paoMap.put(pao.getObjectId(), node);
    }
    return graphList;
  }

  /**
   * Make a pass through the graph checking to see if the evaluation of the change caused any new
   * conflicts. Return the list of conflicts, if any.
   *
   * @return conflictList
   */
  private List<PolicyConflict> computeConflict() {
    List<PolicyConflict> conflictList = new ArrayList<>();
    for (GraphNode node : paoMap.values()) {
      if (computeNodeConflict(node, conflictList)) {
        node.setNewConflict(true);
      }
    }
    return conflictList;
  }

  private boolean computeNodeConflict(GraphNode node, List<PolicyConflict> conflicts) {
    // If the node is not modified, there is nothing to do
    if (!node.isModified()) {
      return false;
    }

    boolean foundNewConflicts = false;
    PolicyInputs initialInputs = node.getInitialPao().getEffectiveAttributes();
    PolicyInputs modifiedInputs = node.getComputePao().getEffectiveAttributes();

    for (var entry : modifiedInputs.getInputs().entrySet()) {
      PolicyInput initialInput = initialInputs.getInputs().get(entry.getKey());
      // If there is no existing matching policy, there can be no conflict
      if (initialInput == null) {
        continue;
      }
      Set<UUID> initialConflicts = initialInput.getConflicts();
      Set<UUID> newConflicts = entry.getValue().getConflicts();
      for (var conflictId : newConflicts) {
        // If the conflict is new - not in the initial state - then we mark the node
        if (!initialConflicts.contains(conflictId)) {
          conflicts.add(
              new PolicyConflict(
                  node.getComputePao(),
                  paoMap.get(conflictId).getComputePao(),
                  initialInput.getPolicyName()));
          foundNewConflicts = true;
        }
      }
    }
    return foundNewConflicts;
  }
}
