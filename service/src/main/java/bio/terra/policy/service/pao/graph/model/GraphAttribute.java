package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.service.pao.model.Pao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GraphAttribute {
    private PolicyInput policyInput; // the attribute
    private final UUID containingPaoId; // the PAO it comes from
    private final Set<UUID> existingConflicts;
    private final Set<UUID> newConflicts;

    public GraphAttribute(UUID containingPaoId, PolicyInput policyInput) {
        this.containingPaoId = containingPaoId;
        this.policyInput = policyInput.duplicateWithoutConflicts();
        existingConflicts = policyInput.getConflicts();
        newConflicts = new HashSet<>();
    }

    public void appendNewConflicts(List<GraphAttributeConflict> conflicts) {
        for (UUID newConflict : newConflicts) {
            conflicts.add(new GraphAttributeConflict(containingPaoId, newConflict, policyInput.getPolicyName()));
        }
    }

    public PolicyInput getPolicyInput() {
        return policyInput;
    }

    public void setPolicyInput(PolicyInput policyInput) {
        this.policyInput = policyInput;
    }

    public UUID getContainingPaoId() {
        return containingPaoId;
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
        return (containingPaoId.equals(changedPaoId));
    }

    public void setNewConflict(UUID conflict) {
        newConflicts.add(conflict);
    }
}
