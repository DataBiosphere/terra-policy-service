package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.service.pao.model.Pao;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GraphAttribute {
  private PolicyInput policyInput; // the attribute
  private final Pao containingPao; // the PAO it comes from
  private final Set<UUID> existingConflicts; // re-found conflicts
  private final Set<UUID> newConflicts; // new conflicts

  public GraphAttribute(Pao containingPao, PolicyInput policyInput) {
    this.containingPao = containingPao;
    this.policyInput = policyInput.duplicateWithoutConflicts();
    existingConflicts = new HashSet<>();
    newConflicts = new HashSet<>();
  }

  public PolicyInput getPolicyInput() {
    return policyInput;
  }

  public void setPolicyInput(PolicyInput policyInput) {
    this.policyInput = policyInput;
  }

  public Pao getContainingPao() {
    return containingPao;
  }

  public Set<UUID> getExistingConflicts() {
    return existingConflicts;
  }

  public Set<UUID> getNewConflicts() {
    return newConflicts;
  }

  public boolean hasExistingConflict() {
    return !existingConflicts.isEmpty();
  }

  public boolean hasNewConflict() {
    return !newConflicts.isEmpty();
  }

  public boolean isChanged(UUID changedPaoId) {
    return (containingPao.equals(changedPaoId));
  }

  public void setNewConflict(UUID conflict) {
    newConflicts.add(conflict);
  }

  public void setExistingConflict(UUID conflict) {
    existingConflicts.add(conflict);
  }
}
