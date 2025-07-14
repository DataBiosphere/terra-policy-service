package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.service.pao.model.Pao;
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
 * effective attribute set. The expected usage is:
 *
 * <ol>
 *   <li>construct the evaluator
 *   <li>add the attributes sets that are to be processed
 *   <li>call evaluate() to perform the evaluation
 * </ol>
 */
public class AttributeEvaluator {
  private final Map<String, List<GraphAttribute>> inputs;
  private final Pao containingPao; // The PAO we are evaluating

  public AttributeEvaluator(Pao containingPao) {
    this.inputs = new HashMap<>();
    this.containingPao = containingPao;
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
      newAttribute = new GraphAttribute(containingPao, input);
      if (attribute.hasNewConflict()) {
        propagateConflict(newAttribute, attribute);
      }
    }

    PolicyInput currentInput = newAttribute.getPolicyInput();
    PolicyInput resultInput = PolicyMutator.combine(currentInput, input);
    // We propagate conflicts through dependents even if the policies are fine otherwise.
    if (resultInput == null || attribute.hasNewConflict()) {
      propagateConflict(newAttribute, attribute);
    } else {
      newAttribute.setPolicyInput(resultInput);
    }
    return newAttribute;
  }

  /**
   * We need to propagate conflicts through dependents, even if there is no conflict on the policy.
   *
   * @param newAttribute attribute we are configuring
   * @param attribute possible source of the conflict
   */
  private void propagateConflict(GraphAttribute newAttribute, GraphAttribute attribute) {
    UUID id = attribute.getContainingPao().getObjectId();
    if (attribute.getPolicyInput().getConflicts().contains(id)) {
      newAttribute.addReFoundConflict(id);
    } else {
      newAttribute.addNewConflict(id);
    }
  }

  private void addAttribute(String key, GraphAttribute graphAttribute) {
    List<GraphAttribute> graphAttributeList = inputs.computeIfAbsent(key, k -> new ArrayList<>());
    graphAttributeList.add(graphAttribute);
  }
}
