package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.service.pao.model.Pao;
import java.util.List;

/**
 * The GraphNode represents one policy attribute object (PAO) in TPS.

 */
public class GraphNode {
  private Pao pao; // Pao being computed in the graph walk
  private List<GraphNode> sources;
  private List<GraphNode> dependents;
  private boolean modified; // true means something has been changed; false means no change
  private boolean
      newConflict; // true means we have computed a new conflict; false means no new conflict
  // TODO: Do we need newConflict, or should we scan the effectiveAttributeSet for the answer instead.

  private GraphAttributeSet effectiveAttributeSet;
  private GraphAttributeSet objectAttributeSet;

  public GraphNode(Pao pao, boolean isModified) {
    this.pao = pao;
    this.effectiveAttributeSet = new GraphAttributeSet(pao.getObjectId(), pao.getEffectiveAttributes());
    this.objectAttributeSet = new GraphAttributeSet(pao.getObjectId(), pao.getAttributes());
    this.modified = isModified;
    this.newConflict = false;
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

  public boolean isNewConflict() {
    return newConflict;
  }

  public void setNewConflict(boolean newConflict) {
    this.newConflict = newConflict;
  }

  public GraphAttributeSet getEffectiveAttributeSet() {
    return effectiveAttributeSet;
  }

  public void setEffectiveAttributeSet(GraphAttributeSet effectiveAttributeSet) {
    this.effectiveAttributeSet = effectiveAttributeSet;
  }

  public GraphAttributeSet getObjectAttributeSet() {
    return objectAttributeSet;
  }

}
