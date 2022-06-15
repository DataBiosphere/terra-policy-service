package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import org.apache.commons.lang3.StringUtils;

public enum PaoComponent {
  WSM("wsm");

  /** Component string used in the database */
  private final String dbComponent;

  PaoComponent(String dbComponent) {
    this.dbComponent = dbComponent;
  }

  public String getDbComponent() {
    return dbComponent;
  }

  public static PaoComponent fromDb(String dbComponent) {
    for (PaoComponent component : PaoComponent.values()) {
      if (StringUtils.equals(dbComponent, component.getDbComponent())) {
        return component;
      }
    }
    throw new InternalTpsErrorException("Invalid component from database");
  }
}
