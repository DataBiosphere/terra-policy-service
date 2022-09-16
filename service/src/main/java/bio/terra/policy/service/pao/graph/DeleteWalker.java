package bio.terra.policy.service.pao.graph;

import bio.terra.policy.db.DbPao;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.service.pao.graph.model.DeleteGraphNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class DeleteWalker {
  private HashMap<UUID, DeleteGraphNode> subgraphMap;
  private PaoDao paoDao;
  private UUID rootObjectId;

  public DeleteWalker(PaoDao paoDao, UUID rootObjectId) {
    subgraphMap = new HashMap<>();
    this.paoDao = paoDao;
    this.rootObjectId = rootObjectId;
  }

  public Set<DbPao> findRemovablePaos() {
    final Set<UUID> dependents = paoDao.getDependentIds(rootObjectId);
    final Set<DbPao> result = new HashSet<>();

    if (dependents.isEmpty()) {
      DbPao dbPao = paoDao.getDbPao(rootObjectId);

      // First Pass: Build a map of all PAOs in our subgraph and note if they are flagged for
      // deletion.
      HashMap<UUID, DeleteGraphNode> subgraphMap = new HashMap<>();
      walkDeleteSubgraph(subgraphMap, dbPao);

      // Second Pass: iterate through our graph, for each PAO:
      // Recursively check its dependents and verify it's still removable.
      // If all dependents are flagged for deletion and exist in our known subgraph,
      // then the PAO is still removable.
      HashMap<UUID, Set<DbPao>> dependentMap = new HashMap<>();
      walkDeleteDependents(subgraphMap, dependentMap, dbPao);

      // Finally - remove PAOs that are still removable
      subgraphMap.forEach(
          (UUID id, DeleteGraphNode node) -> {
            if (node.getRemovable()) {
              result.add(node.getPao());
            }
          });
    }

    return result;
  }

  /**
   * Recursively build out a map of which PAOs are in the subgraph and note which PAOs have been
   * flagged for deletion.
   *
   * @param subgraphMap a map of PAO id to dbPao that will be filled in during the recursive calls
   * @param pao the PAO currently being evaluated. On first call, this would be the root of the
   *     subgraph.
   */
  private void walkDeleteSubgraph(HashMap<UUID, DeleteGraphNode> subgraphMap, DbPao pao) {
    subgraphMap.put(pao.objectId(), new DeleteGraphNode(pao, pao.deleted()));

    for (var source : pao.sources()) {
      walkDeleteSubgraph(subgraphMap, paoDao.getDbPao(UUID.fromString(source)));
    }
  }

  /**
   * Recursive call to check all dependents of a given PAO and update the PAOs removability.
   *
   * @param subgraphMap a map of PAO id to dbPao. This map gets checked for subgraph membership as
   *     we recurse through dependents.
   * @param dependentMap a map of PAO id to all of that PAOs dependents. This serves as a cache and
   *     is filled in during recursive calls.
   * @param pao the PAO currently being evaluated. On first call, this would be the root of the
   *     subgraph.
   */
  private void walkDeleteDependents(
      HashMap<UUID, DeleteGraphNode> subgraphMap,
      HashMap<UUID, Set<DbPao>> dependentMap,
      DbPao pao) {
    HashMap<UUID, DbPao> dependents = new HashMap<>();

    if (pao == null) return;

    if (!dependentMap.containsKey(pao.objectId())) {
      // use a BFS to build a dependency list
      Queue<UUID> queue = new LinkedList<>();
      queue.addAll(paoDao.getDependentIds(pao.objectId()));

      while (!queue.isEmpty()) {
        UUID depId = queue.poll();
        if (!dependents.containsKey(depId)) {
          dependents.put(depId, paoDao.getDbPao(depId));
        }
        queue.addAll(paoDao.getDependentIds(depId));
      }

      dependentMap.put(pao.objectId(), new HashSet<>(dependents.values()));
    }

    for (var dependent : dependents.values()) {
      if (!subgraphMap.containsKey(dependent.objectId()) || !dependent.deleted()) {
        // if any dependent is not part of the subgraph or is not flagged for removal
        // then the current PAO cannot be removed.
        subgraphMap.get(pao.objectId()).setRemovable(false);
        break;
      }
    }

    for (var source : pao.sources()) {
      // recursive step to continue checking all the current PAOs sources
      // use the subgraphMap so lookup PAOs so that we don't keep querying the db
      walkDeleteDependents(
          subgraphMap, dependentMap, subgraphMap.get(UUID.fromString(source)).getPao());
    }
  }
}
