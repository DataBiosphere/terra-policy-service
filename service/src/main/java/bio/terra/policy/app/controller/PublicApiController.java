package bio.terra.policy.app.controller;

import bio.terra.policy.generated.api.PublicApi;
import bio.terra.policy.generated.model.ApiVersionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {
  private final ApiVersionProperties versionProperties;

  @Autowired
  public PublicApiController(ApiVersionProperties versionProperties) {
    this.versionProperties = versionProperties;
    new ApiVersionProperties()
        .build(versionProperties.getBuild())
        .gitHash(versionProperties.getGitHash())
        .github(versionProperties.getGithub())
        .gitTag(versionProperties.getGitTag());
  }

  @Override
  public ResponseEntity<Void> getStatus() {
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<ApiVersionProperties> getVersion() {
    // these copy shenanigans are because versionProperties comes from spring config
    // and is actually a proxy and using the instance directly in a http response includes all the
    // proxy fields that no one wants to see
    return ResponseEntity.ok(
        new ApiVersionProperties()
            .build(versionProperties.getBuild())
            .gitHash(versionProperties.getGitHash())
            .github(versionProperties.getGithub())
            .gitTag(versionProperties.getGitTag()));
  }
}
