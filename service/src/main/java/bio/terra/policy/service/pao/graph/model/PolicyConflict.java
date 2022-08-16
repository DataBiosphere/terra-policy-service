package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.service.pao.model.Pao;

/**
 * Hold a policy conflict from the graph walk. The dependent is the updated dependent Pao based on
 * the proposed update source. The source is the proposed updated source. The policyName is the name
 * of the policy that has the conflict between the two.
 */
public record PolicyConflict(Pao dependent, Pao source, PolicyName policyName) {}
