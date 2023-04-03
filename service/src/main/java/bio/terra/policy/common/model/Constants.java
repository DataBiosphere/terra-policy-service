package bio.terra.policy.common.model;

public class Constants {
  public static final String TERRA_NAMESPACE = "terra";

  public static final PolicyName GROUP_CONSTRAINT_POLICY_NAME =
      new PolicyName(TERRA_NAMESPACE, "group-constraint");
  public static final PolicyName REGION_CONSTRAINT_POLICY_NAME =
      new PolicyName(TERRA_NAMESPACE, "region-constraint");
  public static final PolicyName PROTECTED_DATA_POLICY_NAME =
      new PolicyName(TERRA_NAMESPACE, "protected-data");
}
