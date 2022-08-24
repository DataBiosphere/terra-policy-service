package bio.terra.policy.service.policy.model;

import bio.terra.policy.service.pao.graph.model.PolicyConflict;
import bio.terra.policy.service.pao.model.Pao;
import java.util.List;

public record PolicyUpdateResult(Pao computedPao, List<PolicyConflict> conflicts) {}
