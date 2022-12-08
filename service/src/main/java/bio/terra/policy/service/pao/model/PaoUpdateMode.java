package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.exception.EnumNotRecognizedException;
import bio.terra.policy.generated.model.ApiTpsUpdateMode;

public enum PaoUpdateMode {
  ENFORCE_CONFLICTS(ApiTpsUpdateMode.ENFORCE_CONFLICT),
  FAIL_ON_CONFLICT(ApiTpsUpdateMode.FAIL_ON_CONFLICT),
  DRY_RUN(ApiTpsUpdateMode.DRY_RUN);

  private final ApiTpsUpdateMode apiUpdateMode;

  PaoUpdateMode(ApiTpsUpdateMode apiUpdateMode) {
    this.apiUpdateMode = apiUpdateMode;
  }

  public static PaoUpdateMode fromApi(ApiTpsUpdateMode apiUpdateMode) {
    for (PaoUpdateMode updateMode : PaoUpdateMode.values()) {
      if (apiUpdateMode == updateMode.apiUpdateMode) {
        return updateMode;
      }
    }
    throw new EnumNotRecognizedException("Invalid TpsUpdateMode");
  }
}
