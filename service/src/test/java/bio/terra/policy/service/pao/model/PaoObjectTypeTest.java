package bio.terra.policy.service.pao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.policy.generated.model.ApiTpsObjectType;
import bio.terra.policy.testutils.TestUnitBase;
import org.junit.jupiter.api.Test;

public class PaoObjectTypeTest extends TestUnitBase {
  @Test
  void testFromApi() {
    for (ApiTpsObjectType type : ApiTpsObjectType.values()) {
      assertEquals(type, PaoObjectType.fromApi(type).toApi());
    }
  }

  @Test
  void testFromDb() {
    for (PaoObjectType type : PaoObjectType.values()) {
      assertEquals(type, PaoObjectType.fromDb(type.getDbObjectType()));
    }
  }
}
