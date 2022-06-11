package bio.terra.policy.service.policy;

import bio.terra.policy.common.exception.PolicyNotImplementedException;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.policy.model.CloudPlatform;
import bio.terra.policy.service.policy.model.RegionConstraintResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PolicyService {

  @Autowired
  public PolicyService() {}

  public RegionConstraintResult evaluateRegionConstraint(
      PolicyInputs policyInputs, CloudPlatform cloudPlatform, String regionRequest) {
    throw new PolicyNotImplementedException("Not implemented");
  }
}
