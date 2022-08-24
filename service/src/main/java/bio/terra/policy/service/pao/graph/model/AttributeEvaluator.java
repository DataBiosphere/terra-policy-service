package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.service.policy.PolicyMutator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This code does the processing that turns a group of policy attributes from PAOs into a single,
 * effective attribute set. The expected usage is: 1. construct the evaluator 2. add the attributes
 * sets that are to be processed 3. call evaluate() to perform the evaluation.
 */
public class AttributeEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(AttributeEvaluator.class);
  private final Map<String, List<GraphAttribute>> inputs;

  public AttributeEvaluator() {
    this.inputs = new HashMap<>();
  }

  public void addAttributeSet(GraphAttributeSet attributeSet) {
    for (GraphAttribute attribute : attributeSet.getAttributes()) {
      addAttribute(attribute.getPolicyInput().getKey(), attribute);
    }
  }

  public GraphAttributeSet evaluate(UUID changedPaoId) {
    // Start with an empty attribute set
    var effectiveAttributeSet = new GraphAttributeSet();
    inputs.forEach(
        (k, v) -> effectiveAttributeSet.putAttribute(evaluateOnePolicy(k, v, changedPaoId)));
    return effectiveAttributeSet;
  }

  private GraphAttribute evaluateOnePolicy(
      String key, List<GraphAttribute> attributeList, UUID changedPaoId) {
    // Three settings that control the order we combine policies on our list:
    //  - is it changed (does the changedPaoId != the containingPao id)
    //  - does it have existing conflicts
    //  - does it have new conflicts
    //
    // We processd in order:
    // 1. unchanged, no existing conflicts, no new conflicts
    // 2. unchanged, existing conflicts, no new conflicts
    // 3. unchanged, new conflicts (both existing cases are done)
    // 4. changed items
    //
    // We remove items from the list as they are combined in, so we don't have to filter
    // them out.

    GraphAttribute newAttribute = null;

    // Combo 1: unchanged, no existing conflicts, no new conflicts
    List<GraphAttribute> remainingList1 = new ArrayList<>();
    for (GraphAttribute attribute : attributeList) {
      if (attribute.isChanged(changedPaoId)
          || attribute.hasNewConflict()
          || attribute.hasExistingConflict()) {
        remainingList1.add(attribute);
      } else {
        newAttribute = combineAttribute(newAttribute, attribute);
      }
    }

    // Combo 2: unchanged, existing conflicts, no new conflicts
    List<GraphAttribute> remainingList2 = new ArrayList<>();
    for (GraphAttribute attribute : remainingList1) {
      if (attribute.isChanged(changedPaoId) || attribute.hasNewConflict()) {
        remainingList2.add(attribute);
      } else {
        newAttribute = combineAttribute(newAttribute, attribute);
      }
    }

    // Combo 3: unchanged, new conflicts
    List<GraphAttribute> remainingList3 = new ArrayList<>();
    for (GraphAttribute attribute : remainingList2) {
      if (attribute.isChanged(changedPaoId) || attribute.hasNewConflict()) {
        remainingList3.add(attribute);
      } else {
        newAttribute = combineAttribute(newAttribute, attribute);
      }
    }

    for (GraphAttribute attribute : remainingList3) {
      newAttribute = combineAttribute(newAttribute, attribute);
    }

    return newAttribute;
  }

  // combine attribute into newAttribute
  private GraphAttribute combineAttribute(GraphAttribute newAttribute, GraphAttribute attribute) {
    PolicyInput input = attribute.getPolicyInput();
    if (newAttribute == null) {
      return new GraphAttribute(attribute.getContainingPao(), input);
    }

    PolicyInput currentInput = newAttribute.getPolicyInput();
    PolicyInput resultInput = PolicyMutator.combine(currentInput, input);
    // We propagate conflicts through dependents even if the policies are fine otherwise.
    if (resultInput == null || attribute.hasNewConflict()) {
      UUID id = attribute.getContainingPao().getObjectId();
      if (attribute.getPolicyInput().getConflicts().contains(id)) {
        newAttribute.setExistingConflict(id);
      } else {
        newAttribute.setNewConflict(id);
      }
    } else {
      newAttribute.setPolicyInput(resultInput);
    }
    return newAttribute;
  }

  private void addAttribute(String key, GraphAttribute graphAttribute) {
    List<GraphAttribute> graphAttributeList = inputs.computeIfAbsent(key, k -> new ArrayList<>());
    graphAttributeList.add(graphAttribute);
  }
}
