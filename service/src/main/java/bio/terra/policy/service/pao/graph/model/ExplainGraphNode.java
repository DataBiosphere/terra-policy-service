package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyInput;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The ExplainGraphNode represents the contributions of one object to a policy. */
public class ExplainGraphNode {
  private final UUID objectId;
  private final PolicyInput policyInput;
  private final List<ExplainGraphNode> sources;

  public ExplainGraphNode(UUID objectId, PolicyInput policyInput) {
    this.objectId = objectId;
    this.policyInput = policyInput;
    this.sources = new ArrayList<>();
  }

  public UUID getObjectId() {
    return objectId;
  }

  public PolicyInput getPolicyInput() {
    return policyInput;
  }

  public List<ExplainGraphNode> getSources() {
    return sources;
  }

  public void addSourceNode(ExplainGraphNode node) {
    this.sources.add(node);
  }
}
