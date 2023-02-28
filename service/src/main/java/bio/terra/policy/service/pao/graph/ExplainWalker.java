package bio.terra.policy.service.pao.graph;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.service.pao.graph.model.ExplainGraph;
import bio.terra.policy.service.pao.graph.model.ExplainGraphNode;
import bio.terra.policy.service.pao.model.Pao;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Walk the sources building explain nodes. We build one explain tree for each policy input in the
 * targetPao effective attributes.
 *
 * <p>The explanation for a Pao contains its policyInput from the effectiveAttributes. When this is
 * a leaf node of the tree, that will be the same as the set attributes.
 *
 * <p>The explain() method walks the sources for a single policy name. If there are sources, we
 * recurse. A given PAO may be represented twice: as an ExplainGraphNode for its contributions to
 * dependent policies and in the sources list for its specific contributions. Other contributions
 *
 * <p>If we have this:
 *
 * <pre>
 * WS: set: D; effective: A, B, C, D; sources: [DC1, DC2]
 * DC1: set: A; effective: A; sources: []
 * DC2: set: B; effective: B, C; sources: [DC3]
 * DC3: set: C; effective: C; sources: []
 * </pre>
 *
 * <p>The explain graph will look like:
 *
 * <pre>
 *   ExplainGraph:
 *     explainPaos: [WS-pao, DC1-pao, DC2-pao, DC3-pao]
 *     explainGraph: [
 *       ExplainGraphNode:
 *         objectId: WS-id
 *         policyInput: [A, B, C, D]
 *         sources: [
 *           ExplainGraphNode:
 *             objectId: WS-id
 *             policyInput: [D]
 *             sources: []
 *           ExplainGraphNode:
 *             objectId: DC1-id
 *             policyInput: [A]
 *             sources: []
 *           ExplainGraphNode:
 *             objectId: DC2-id
 *             policyInput: [B, C]
 *             sources: [
 *               ExplainGraphNode:
 *                 objectId: DC2-id
 *                 policyInput: [B]
 *                 sources: []
 *               ExplainGraphNode:
 *                 objectId: DC3-id
 *                 policyInput: [C]
 *                 sources: []
 * </pre>
 *
 * <p>You might ask, why do some PAOs appear twice?
 *
 * <p>The concept is that the ExplainGraphNode policyInput is what is contributed to the next layer
 * up. So for DC2, you see that it contributes [B, C] up to WS. But DC2 sources are itself,
 * contributing B, and DC3 contributing C.
 *
 * <p>Essentially, the "contributes" is the effective policy and the "source is me" is the set
 * policy on the object. Notice that the leaf nodes do not contain themselves as sources. That is
 * only done when there are other contributing sources. Otherwise, the graph would be infinitely
 * deep!
 */
public class ExplainWalker {
  private final PaoDao paoDao;
  private final Map<UUID, Pao> paoMap;
  private final int depth;
  private final List<ExplainGraphNode> graph;

  /**
   * Constructor and execution of the ExplainWalker. The resulting explain graph is computed as part
   * of construction and then is returned in a getter. A new walker is constructed for each walk.
   *
   * @param paoDao for retrieving PAO objects from the database
   * @param targetObjectId the objectId of the PAO to explain
   * @param depth depth to recurse; 0 means no depth limit
   */
  public ExplainWalker(PaoDao paoDao, UUID targetObjectId, int depth) {
    this.paoDao = paoDao;
    this.depth = depth;
    this.paoMap = new HashMap<>();

    Pao targetPao = getPao(targetObjectId);
    this.graph = new ArrayList<>();
    for (PolicyInput policyInput : targetPao.getEffectiveAttributes().getInputs().values()) {
      graph.add(explain(targetPao.getObjectId(), policyInput.getPolicyName(), 0));
    }
  }

  /** @return Return the graph in two parts */
  public ExplainGraph getExplainGraph() {
    return new ExplainGraph(graph, paoMap.values());
  }

  /**
   * This method builds a tree of explain nodes for a particular policy, stopping at a specific
   * depth. Depth == 0 means full expansion; otherwise, we go depth levels down.
   *
   * @param objectId UUID of a PAO
   * @param policyName name of the policy we are working on
   * @param currentDepth current recursion depth; starts at 0
   * @return graph node or null if there is no contribution to the policy from this PAO
   */
  private @Nullable ExplainGraphNode explain(
      UUID objectId, PolicyName policyName, int currentDepth) {
    Pao pao = getPao(objectId);

    // See if the policy is in the effective attributes. If not, we have no work to do.
    PolicyInput input = pao.getEffectiveAttributes().lookupPolicy(policyName);
    if (input == null) {
      return null;
    }

    // OK, we have an explain graph node to make. This reflects the effective policy
    // at this level.
    ExplainGraphNode node = new ExplainGraphNode(objectId, input);

    // If we have a depth constraint and we are at it, do not recurse further
    if (depth != 0 && depth <= currentDepth) {
      return node;
    }

    // Recurse through each source Pao building explain nodes.
    for (UUID sourceId : pao.getSourceObjectIds()) {
      ExplainGraphNode sourceNode = explain(sourceId, policyName, currentDepth + 1);
      if (sourceNode != null) {
        node.addSourceNode(sourceNode);
      }
    }

    // If there are contributors under us, we have to add our contribution. That lets
    // callers see the contributions that came from this PAO vs ones inherited from
    // other sources
    if (!node.getSources().isEmpty()) {
      PolicyInput setInput = pao.getAttributes().lookupPolicy(policyName);
      if (setInput != null) {
        ExplainGraphNode setNode = new ExplainGraphNode(objectId, setInput);
        node.addSourceNode(setNode);
      }
    }

    return node;
  }

  /**
   * Get a PAO with the requested objectId. This will get the PAO from the paoMap if it is there.
   * Otherwise, it reads from the database. Since we visit the same PAOs repeatedly in this
   * algorithm, that seems worthwhile.
   *
   * @param objectId UUID of a PAO
   * @return the associated PAO
   */
  private Pao getPao(UUID objectId) {
    Pao pao = paoMap.get(objectId);
    if (pao == null) {
      pao = paoDao.getPao(objectId);
      paoMap.put(objectId, pao);
    }
    return pao;
  }
}
