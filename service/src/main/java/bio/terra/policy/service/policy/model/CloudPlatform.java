package bio.terra.policy.service.policy.model;

import bio.terra.policy.model.ApiCloudPlatform;
import bio.terra.policy.service.policy.exception.InvalidInputException;

public enum CloudPlatform {
  AZURE(ApiCloudPlatform.AZURE, "azure"),
  GCP(ApiCloudPlatform.GCP, "gcp");

  /** interface platform */
  ApiCloudPlatform apiCloudPlatform;
  /** platform string used in the rego code */
  String regoCloudPlatform;

  CloudPlatform(ApiCloudPlatform apiPlatform, String regoPlatform) {
    this.apiCloudPlatform = apiPlatform;
    this.regoCloudPlatform = regoPlatform;
  }

  public static CloudPlatform fromApi(ApiCloudPlatform apiPlatform) {
    for (CloudPlatform platform : CloudPlatform.values()) {
      if (platform.getApiCloudPlatform() == apiPlatform) {
        return platform;
      }
    }
    throw new InvalidInputException("Invalid cloud platform: " + apiPlatform);
  }

  public ApiCloudPlatform getApiCloudPlatform() {
    return apiCloudPlatform;
  }

  public String getRegoCloudPlatform() {
    return regoCloudPlatform;
  }
}
