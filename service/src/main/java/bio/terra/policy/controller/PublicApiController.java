package bio.terra.policy.controller;

import bio.terra.tps.generated.api.PublicApi;
import bio.terra.tps.generated.model.VersionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PublicApiController implements PublicApi {
  final private VersionProperties versionProperties;

  @Autowired
  public PublicApiController(VersionProperties versionProperties) {
    this.versionProperties = versionProperties;
  }

  @Override
  public ResponseEntity<Void> getStatus() {
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<VersionProperties> getVersion() {
    return ResponseEntity.ok(versionProperties);
  }
}
