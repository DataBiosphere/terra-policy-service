package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.service.pao.model.Pao;

/**
 * Hold a policy conflict from the graph walk. The `pao` is the pao whose effective attribute set is
 * being evaluated. The `conflictPao` is the pao that cause the conflict. Note that it is possible
 * for the `conflictPao` to be the `pao` if the conflict was caused by a change in the `pao`s
 * attributes.
 */
public record PolicyConflict(Pao pao, Pao conflictPao, PolicyName policyName) {}
