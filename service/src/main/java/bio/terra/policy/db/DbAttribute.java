package bio.terra.policy.db;

import bio.terra.policy.common.model.PolicyInput;

/** Record to hold one attribute of an attribute set and its set id */
public record DbAttribute(String setId, PolicyInput policyInput) {}
