package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Representation of the attribute set for purposes of walking the graph. */
public class GraphAttributeSet {
  private final Map<String, GraphAttribute> attributeSet;

  public GraphAttributeSet() {
    this.attributeSet = new HashMap<>();
  }

  public GraphAttributeSet(Pao pao, PolicyInputs inputs) {
    this.attributeSet = new HashMap<>();
    for (PolicyInput input : inputs.getInputs().values()) {
      attributeSet.put(input.getKey(), new GraphAttribute(pao, input));
    }
  }

  public Collection<GraphAttribute> getAttributes() {
    return attributeSet.values();
  }

  public void putAttribute(GraphAttribute graphAttribute) {
    attributeSet.put(graphAttribute.getPolicyInput().getKey(), graphAttribute);
  }

  /**
   * Make a policy attribute set from the graph attribute set
   *
   * @return new attribute set
   */
  public PolicyInputs makeAttributeSet() {
    var inputs = new PolicyInputs();
    for (GraphAttribute attribute : getAttributes()) {
      Set<UUID> combinedConflicts = new HashSet<>(attribute.getReFoundConflicts());
      combinedConflicts.addAll(attribute.getNewConflicts());
      var input =
          new PolicyInput(
              attribute.getPolicyInput().getPolicyName(),
              attribute.getPolicyInput().getAdditionalData(),
              combinedConflicts);
      inputs.addInput(input);
    }
    return inputs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GraphAttributeSet)) return false;
    GraphAttributeSet that = (GraphAttributeSet) o;
    return Objects.equals(attributeSet, that.attributeSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributeSet);
  }
}
