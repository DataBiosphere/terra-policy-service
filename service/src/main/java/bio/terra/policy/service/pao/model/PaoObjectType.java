package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.exception.EnumNotRecognizedException;
import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.generated.model.ApiTpsObjectType;
import org.apache.commons.lang3.StringUtils;

public enum PaoObjectType {
  DATASET("dataset", ApiTpsObjectType.DATASET),
  SNAPSHOT("snapshot", ApiTpsObjectType.SNAPSHOT),
  BILLING_PROFILE("billing-profile", ApiTpsObjectType.BILLING_PROFILE),
  WORKSPACE("workspace", ApiTpsObjectType.WORKSPACE);

  /** Object type string used in the database */
  private final String dbObjectType;
  /** API object type */
  private final ApiTpsObjectType apiObjectType;

  PaoObjectType(String dbObjectType, ApiTpsObjectType apiObjectType) {
    this.dbObjectType = dbObjectType;
    this.apiObjectType = apiObjectType;
  }

  public ApiTpsObjectType toApi() {
    return apiObjectType;
  }

  public static PaoObjectType fromApi(ApiTpsObjectType apiObjectType) {
    for (PaoObjectType objectType : PaoObjectType.values()) {
      if (apiObjectType == objectType.apiObjectType) {
        return objectType;
      }
    }
    throw new EnumNotRecognizedException("Invalid TpsObjectType");
  }

  public String getDbObjectType() {
    return dbObjectType;
  }

  public static PaoObjectType fromDb(String dbObjectType) {
    for (PaoObjectType ObjectType : PaoObjectType.values()) {
      if (StringUtils.equals(dbObjectType, ObjectType.getDbObjectType())) {
        return ObjectType;
      }
    }
    throw new InternalTpsErrorException("Invalid ObjectType from database");
  }
}
