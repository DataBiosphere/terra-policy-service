package bio.terra.policy.service.policy.model;

import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import java.util.List;

public record ChangeDescription(
    PolicyInputs newEffectiveAttributes, List<PolicyConflict> conflicts) {}
