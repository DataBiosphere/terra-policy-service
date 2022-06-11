package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.model.ApiTerraObjectType;
import org.apache.commons.lang3.StringUtils;

public enum PaoObjectType {
  WORKSPACE(ApiTerraObjectType.WORKSPACE, "workspace");

  /** Object type used in the REST API */
  private final ApiTerraObjectType apiTerraObjectType;

  /** Object type string used in the database */
  private final String dbObjectType;

  PaoObjectType(ApiTerraObjectType apiTerraObjectType, String dbObjectType) {
    this.apiTerraObjectType = apiTerraObjectType;
    this.dbObjectType = dbObjectType;
  }

  public ApiTerraObjectType getApiTerraObjectType() {
    return apiTerraObjectType;
  }

  public String getDbObjectType() {
    return dbObjectType;
  }

  public static PaoObjectType fromApi(ApiTerraObjectType apiTerraObjectType) {
    for (PaoObjectType ObjectType : PaoObjectType.values()) {
      if (apiTerraObjectType == ObjectType.getApiTerraObjectType()) {
        return ObjectType;
      }
    }
    throw new InvalidInputException("Invalid TerraObjectType");
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
