package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.common.model.PolicyName;

import java.util.UUID;

public record GraphAttributeConflict(UUID containingPaoId, UUID conflictingPaoId, PolicyName policyName) {}
