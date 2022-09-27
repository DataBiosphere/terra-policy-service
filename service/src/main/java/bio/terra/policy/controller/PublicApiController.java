package bio.terra.policy.controller;

import bio.terra.policy.generated.api.PublicApi;
import bio.terra.policy.generated.model.VersionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {
  private final VersionProperties versionProperties;

  @Autowired
  public PublicApiController(VersionProperties versionProperties) {
    this.versionProperties = versionProperties;
    new VersionProperties()
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
  public ResponseEntity<VersionProperties> getVersion() {
    // these copy shenanigans are because versionProperties comes from spring config
    // and is actually a proxy and using the instance directly in a http response includes all the
    // proxy fields that no one wants to see
    return ResponseEntity.ok(
        new VersionProperties()
            .build(versionProperties.getBuild())
            .gitHash(versionProperties.getGitHash())
            .github(versionProperties.getGithub())
            .gitTag(versionProperties.getGitTag()));
  }
}
