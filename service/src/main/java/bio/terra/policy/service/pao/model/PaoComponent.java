package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.exception.EnumNotRecognizedException;
import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.generated.model.ApiTpsComponent;
import org.apache.commons.lang3.StringUtils;

public enum PaoComponent {
  WSM("wsm", ApiTpsComponent.WSM);

  /** Component string used in the database */
  private final String dbComponent;
  /** Component enum used in teh API */
  private final ApiTpsComponent apiComponent;

  PaoComponent(String dbComponent, ApiTpsComponent apiComponent) {
    this.dbComponent = dbComponent;
    this.apiComponent = apiComponent;
  }

  public String getDbComponent() {
    return dbComponent;
  }

  public ApiTpsComponent toApi() {
    return apiComponent;
  }

  public static PaoComponent fromApi(ApiTpsComponent apiComponent) {
    for (PaoComponent component : PaoComponent.values()) {
      if (apiComponent == component.apiComponent) {
        return component;
      }
    }
    throw new EnumNotRecognizedException("Invalid TpsComponent");
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
