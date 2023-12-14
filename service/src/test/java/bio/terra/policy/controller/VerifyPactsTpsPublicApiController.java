package bio.terra.policy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import bio.terra.policy.app.controller.PublicApiController;
import bio.terra.policy.app.controller.TpsApiController;
import bio.terra.policy.generated.model.ApiTpsPolicyInput;
import bio.terra.policy.generated.model.ApiTpsPolicyInputs;
import bio.terra.policy.generated.model.ApiTpsPolicyPair;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@Tag("Pact")
@WebMvcTest
@ContextConfiguration(classes = {PublicApiController.class, MvcUtils.class})
@Provider("tps")
// @PactBroker()
@PactFolder("pacts")
class VerifyPactsTpsPublicApiController {

  @MockBean private PublicApiController publicApiController;
  @MockBean private TpsApiController tpsApiController;

  @Autowired private MvcUtils mvcUtils;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String GROUP = "group";
  private static final String REGION = "region-name";
  private static final String DDGROUP = "ddgroup";
  private static final String US_REGION = "usa";
  private static final String IOWA_REGION = "iowa";

  private String checkInitialLastUpdate(UUID paoId) throws Exception {
    var apiPao = mvcUtils.getPao(paoId);
    assertEquals(apiPao.getCreatedDate(), apiPao.getLastUpdatedDate());
    return apiPao.getLastUpdatedDate();
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(new MockMvcTestTarget(mockMvc));
  }

  /*
  Current TPS Contract Testing States:

  "Tps is ok"
  "Tps is not ok"
  "an existing policy" -> "one policy exists"
  "another existing policy" -> "two policies exist"

  */

  @State({"tpsIsOk"})
  public void statusIsOk() throws Exception {
    this.mockMvc.perform(get("/status")).andExpect(status().isOk());
  }

  @State({"tpsIsNotOk"})
  public void statusIsNotOk() throws Exception {}

  @State({"a group policy exists"})
  public void groupPolicyExists() throws Exception {

    // create a policy limited by group
    var groupPolicy =
        new ApiTpsPolicyInput()
            .namespace(TERRA)
            .name(GROUP_CONSTRAINT)
            .addAdditionalDataItem(new ApiTpsPolicyPair().key(GROUP).value(DDGROUP));

    var inputs = new ApiTpsPolicyInputs().addInputsItem(groupPolicy);

    // Create a PAO
    UUID paoIdA = mvcUtils.createPao(inputs);
    String lastUpdated = checkInitialLastUpdate(paoIdA);
  }

  @State({"a region policy exists"})
  public void regionPolicyExists() throws Exception {
    // create a policy limited by region
    var regionPolicy =
        new ApiTpsPolicyInput()
            .namespace(TERRA)
            .name(REGION_CONSTRAINT)
            .addAdditionalDataItem(new ApiTpsPolicyPair().key(REGION).value(US_REGION));

    var inputs = new ApiTpsPolicyInputs().addInputsItem(regionPolicy);

    // Create a PAO
    UUID paoIdA = mvcUtils.createPao(inputs);
  }
}
