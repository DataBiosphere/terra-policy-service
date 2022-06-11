package bio.terra.policy.service.pao;

import bio.terra.policy.app.App;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.model.ApiPolicyInput;
import bio.terra.policy.model.ApiPolicyInputs;
import bio.terra.policy.model.ApiPolicyPair;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest(classes = App.class)
@AutoConfigureMockMvc
public class PaoServiceTest {

  @Autowired private ObjectMapper objectMapper;
  @Autowired private PaoService paoService;

  @Test
  void createPaoTest() throws Exception {

    var objectId = UUID.randomUUID();
    var apiInputs =
        new ApiPolicyInputs()
            .addInputsItem(
                new ApiPolicyInput()
                    .namespace("terra")
                    .name("group-constraint")
                    .addAdditionalDataItem(new ApiPolicyPair().key("group").value("ddgroup")))
            .addInputsItem(
                new ApiPolicyInput()
                    .namespace("terra")
                    .name("region-constraint")
                    .addAdditionalDataItem(new ApiPolicyPair().key("region").value("US")));

    var inputs = PolicyInputs.fromApi(apiInputs);

    paoService.createPao(objectId, PaoComponent.WSM, PaoObjectType.WORKSPACE, inputs);

    // TODO: When I implement GET and DELETE, add the rest of the test...
  }
}
