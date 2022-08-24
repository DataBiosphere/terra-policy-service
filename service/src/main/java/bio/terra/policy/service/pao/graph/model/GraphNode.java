package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import java.util.List;

/** The GraphNode represents one policy attribute object (PAO) in TPS. */
public class GraphNode {
  private final Pao pao; // Pao being computed in the graph walk
  private final GraphAttributeSet objectAttributeSet;
  private GraphAttributeSet effectiveAttributeSet;
  private List<GraphNode> sources;
  private List<GraphNode> dependents;
  private boolean modified; // true means something has been changed; false means no change

  public GraphNode(Pao pao, boolean isModified) {
    this.pao = pao;
    this.effectiveAttributeSet = new GraphAttributeSet(pao, pao.getEffectiveAttributes());
    this.objectAttributeSet = new GraphAttributeSet(pao, pao.getAttributes());
    this.modified = isModified;
  }

  public Pao getPao() {
    return pao;
  }

  public List<GraphNode> getSources() {
    return sources;
  }

  public void setSources(List<GraphNode> sources) {
    this.sources = sources;
  }

  public List<GraphNode> getDependents() {
    return dependents;
  }

  public void setDependents(List<GraphNode> dependents) {
    this.dependents = dependents;
  }

  public boolean isModified() {
    return modified;
  }

  public void setModified(boolean modified) {
    this.modified = modified;
  }

  public GraphAttributeSet getEffectiveAttributeSet() {
    return effectiveAttributeSet;
  }

  public void setEffectiveAttributeSet(GraphAttributeSet effectiveAttributeSet) {
    this.effectiveAttributeSet = effectiveAttributeSet;
  }

  public PolicyInputs getPolicyAttributes() {
    return objectAttributeSet.makeAttributeSet();
  }

  public PolicyInputs getEffectivePolicyAttributes() {
    return effectiveAttributeSet.makeAttributeSet();
  }
}
