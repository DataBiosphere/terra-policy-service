package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.service.pao.model.Pao;
import java.util.List;

public class GraphNode {
  private Pao initialPao; // starting Pao - typically from the database
  private Pao computePao; // Pao being computed in the graph walk
  private List<GraphNode> sources;
  private List<GraphNode> dependents;
  private boolean modified; // true means something has been changed; false means no change
  private boolean
      newConflict; // true means we have computed a new conflict; false means no new conflict

  public Pao getInitialPao() {
    return initialPao;
  }

  public GraphNode setInitialPao(Pao initialPao) {
    this.initialPao = initialPao;
    return this;
  }

  public Pao getComputePao() {
    return computePao;
  }

  public GraphNode setComputePao(Pao computePao) {
    this.computePao = computePao;
    return this;
  }

  public List<GraphNode> getSources() {
    return sources;
  }

  public GraphNode setSources(List<GraphNode> sources) {
    this.sources = sources;
    return this;
  }

  public List<GraphNode> getDependents() {
    return dependents;
  }

  public GraphNode setDependents(List<GraphNode> dependents) {
    this.dependents = dependents;
    return this;
  }

  public boolean isModified() {
    return modified;
  }

  public GraphNode setModified(boolean modified) {
    this.modified = modified;
    return this;
  }

  public boolean isNewConflict() {
    return newConflict;
  }

  public GraphNode setNewConflict(boolean newConflict) {
    this.newConflict = newConflict;
    return this;
  }
}
