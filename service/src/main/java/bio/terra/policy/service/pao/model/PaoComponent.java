package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.model.ApiTerraComponent;
import org.apache.commons.lang3.StringUtils;

public enum PaoComponent {
  WSM(ApiTerraComponent.WSM, "wsm");

  /** Component string used in the REST API */
  private final ApiTerraComponent apiTerraComponent;

  /** Component string used in the database */
  private final String dbComponent;

  PaoComponent(ApiTerraComponent apiTerraComponent, String dbComponent) {
    this.apiTerraComponent = apiTerraComponent;
    this.dbComponent = dbComponent;
  }

  public ApiTerraComponent getApiTerraComponent() {
    return apiTerraComponent;
  }

  public String getDbComponent() {
    return dbComponent;
  }

  public static PaoComponent fromApi(ApiTerraComponent apiTerraComponent) {
    for (PaoComponent component : PaoComponent.values()) {
      if (apiTerraComponent == component.getApiTerraComponent()) {
        return component;
      }
    }
    throw new InvalidInputException("Invalid TerraComponent");
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
