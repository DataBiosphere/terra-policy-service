package bio.terra.policy.service.pao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.policy.generated.model.ApiTpsComponent;
import bio.terra.policy.testutils.TestUnitBase;
import org.junit.jupiter.api.Test;

public class PaoComponentTest extends TestUnitBase {
  @Test
  void testFromApi() {
    for (ApiTpsComponent type : ApiTpsComponent.values()) {
      assertEquals(type, PaoComponent.fromApi(type).toApi());
    }
  }

  @Test
  void testFromDb() {
    for (PaoComponent type : PaoComponent.values()) {
      assertEquals(type, PaoComponent.fromDb(type.getDbComponent()));
    }
  }
}
