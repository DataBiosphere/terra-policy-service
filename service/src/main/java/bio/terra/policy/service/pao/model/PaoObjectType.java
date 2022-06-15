package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import org.apache.commons.lang3.StringUtils;

public enum PaoObjectType {
  WORKSPACE("workspace");

  /** Object type string used in the database */
  private final String dbObjectType;

  PaoObjectType(String dbObjectType) {
    this.dbObjectType = dbObjectType;
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
