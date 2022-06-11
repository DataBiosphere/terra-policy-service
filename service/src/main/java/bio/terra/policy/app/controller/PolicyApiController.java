package bio.terra.policy.app.controller;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.policy.api.PolicyApi;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.model.ApiRegionConstraintRequest;
import bio.terra.policy.model.ApiRegionConstraintResult;
import bio.terra.policy.service.policy.PolicyService;
import bio.terra.policy.service.policy.model.CloudPlatform;
import bio.terra.policy.service.policy.model.RegionConstraintResult;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PolicyApiController implements PolicyApi {

  private final AuthenticatedUserRequestFactory authenticatedUserRequestFactory;
  private final HttpServletRequest request;
  private final PolicyService policyService;

  @Autowired
  public PolicyApiController(
      AuthenticatedUserRequestFactory authenticatedUserRequestFactory,
      HttpServletRequest request,
      PolicyService policyService) {
    this.authenticatedUserRequestFactory = authenticatedUserRequestFactory;
    this.request = request;
    this.policyService = policyService;
  }

  @Override
  public ResponseEntity<ApiRegionConstraintResult> policyRegionConstraint(
      ApiRegionConstraintRequest body) {

    RegionConstraintResult result =
        policyService.evaluateRegionConstraint(
            PolicyInputs.fromApi(body.getPolicyInputs()),
            CloudPlatform.fromApi(body.getCloudPlatform()),
            body.getRegionRequest());

    return PolicyApi.super.policyRegionConstraint(body);
  }

  private AuthenticatedUserRequest getAuthenticatedInfo() {
    return authenticatedUserRequestFactory.from(request);
  }
}
