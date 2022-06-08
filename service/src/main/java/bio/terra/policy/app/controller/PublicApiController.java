package bio.terra.policy.app.controller;

import bio.terra.policy.api.PublicApi;
import bio.terra.policy.app.configuration.VersionConfiguration;
import bio.terra.policy.model.ApiVersionProperties;
import bio.terra.policy.service.status.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {
  private final StatusService statusService;
  private final ApiVersionProperties currentVersion;

  @Autowired
  public PublicApiController(
      StatusService statusService, VersionConfiguration versionConfiguration) {
    this.statusService = statusService;
    this.currentVersion =
        new ApiVersionProperties()
            .gitTag(versionConfiguration.getGitTag())
            .gitHash(versionConfiguration.getGitHash())
            .github(versionConfiguration.getGithub())
            .build(versionConfiguration.getBuild());
  }

  @Override
  public ResponseEntity<Void> serviceStatus() {
    return new ResponseEntity<>(
        statusService.getCurrentStatus() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Override
  public ResponseEntity<ApiVersionProperties> serviceVersion() {
    return new ResponseEntity<>(currentVersion, HttpStatus.OK);
  }
}
