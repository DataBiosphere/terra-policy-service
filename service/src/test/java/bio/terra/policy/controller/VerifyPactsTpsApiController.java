package bio.terra.policy.controller;

import static org.mockito.Answers.CALLS_REAL_METHODS;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import bio.terra.policy.app.PolicySpringApplication;
import bio.terra.policy.app.StartupInitializer;
import bio.terra.policy.app.configuration.TpsDatabaseConfiguration;
import bio.terra.policy.app.controller.PublicApiController;
import bio.terra.policy.app.controller.TpsApiController;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.generated.model.*;
import bio.terra.policy.service.pao.PaoService;
import bio.terra.policy.service.region.RegionService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Tag("Pact")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = PolicySpringApplication.class,
    properties = {
      "policy.policy-database.initialize-on-start=false",
      "policy.policy-database.upgrade-on-start=false",
      // Disable instrumentation for spring-webmvc because pact still uses javax libs which causes
      // opentelemetry to try to load the same bean name twice, once for javax and once for jakarta
      "otel.instrumentation.spring-webmvc.enabled=false"
    })
@EnableAutoConfiguration()
// @EnableAutoConfiguration(
//        exclude = {DataSourceAutoConfiguration.class, JdbcRepositoriesAutoConfiguration.class})

// @LocalServerPort
// int port;
// @WebMvcTest
// @ContextConfiguration(
//    classes = {
//      PublicApiController.class,
//      TpsApiController.class,
//      MvcUtils.class,
//      ConversionUtils.class,
//      PaoService.class
//    })
@Provider("tps")
// @PactBroker()
@PactFolder("pacts")
class VerifyPactsTpsApiController {

  // TODO
  // mock daos and services, then mock anything that blows up (things used by those)
  // do not mock controller
  // "mock data, not behavior"

  // in bpm the states are only handled one at a time, but theoretically states can be combined
  // probably should look into that for here, but if none of the consumers need it, might not  be
  // crucial

  @Autowired private PublicApiController publicApiController;
  @Autowired private TpsApiController tpsApiController;

  @MockBean private MvcUtils mvcUtils;
  // This mockMVC is what we use to test API requests and responses:
  //  @Autowired private MockMvc mockMvc;

  @MockBean private StartupInitializer startupInitializer;
  @MockBean private TpsDatabaseConfiguration tpsDatabaseConfiguration;
  @MockBean private PaoService paoService;
  @MockBean NamedParameterJdbcTemplate tpsJdbcTemplate;

  @MockBean(answer = CALLS_REAL_METHODS)
  private PaoDao paoDao;

  @MockBean private ApiVersionProperties apiVersionProperties;
  @MockBean private RegionService regionService;

  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String WORKSPACE_CONSTRAINT = "workspace-constraint";
  private static final String WORKSPACE = "workspace";
  private static final String WORKSPACE_ID = "workspace-id-fake";
  private static final String GROUP = "group";
  private static final String REGION = "region-name";
  private static final String DDGROUP = "ddgroup";
  private static final String US_REGION = "usa";
  private static final String IOWA_REGION = "iowa";

  //  private String checkInitialLastUpdate(UUID paoId) throws Exception {
  //    var apiPao = mvcUtils.getPao(paoId);
  //    assertEquals(apiPao.getCreatedDate(), apiPao.getLastUpdatedDate());
  //    return apiPao.getLastUpdatedDate();
  //  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    //    context.setTarget(new MockMvcTestTarget(mockMvc));
  }

  /*
  Current TPS Contract Testing States:

  "tpsIsOk" n
  "tpisIsNotOk"
  "createPao"
  "an existing policy" -> "one policy exists" -> "groupPolicyExists"
  "another existing policy" -> "two policies exist" -> "regionPolicyExists"
  */

  @State({"tpsIsOk"})
  public void statusIsOk() throws Exception {}

  @State({"no pao exists"})
  public void noPaoExists() throws Exception {

    //    when(mockPaoDao.createPao();

    //    var testPolicy = new ApiTpsPolicyInput().namespace("testNamespace").name("testName");
    //
    //    var inputs = new ApiTpsPolicyInputs().addInputsItem(testPolicy);

    //    var createRequest =
    //        new ApiTpsPaoCreateRequest()
    //            .component(ApiTpsComponent.valueOf("BPM"))
    //            .objectType(ApiTpsObjectType.valueOf("BILLING_PROFILE"))
    //            .objectId(UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"))
    //            .attributes(inputs);
    //
    //    //     Create a PAO
    //    tpsApiController.createPao(createRequest);

    //    mockPaoService.deletePao(any());
    //
    //    when(tpsApiController.deletePao(any())).thenReturn(new
    // ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @State({"a group policy exists"})
  public void aGroupPolicyExists() throws Exception {

    //     copied from tdr helper function
    //         private UUID idFromParameters(Map<?, ?> parameters) {
    //        return UUID.fromString(parameters.get("id").toString());
    //
    //    // create a policy limited by group
    //    var groupPolicy =
    //        new ApiTpsPolicyInput()
    //            .namespace(TERRA)
    //            .name(GROUP_CONSTRAINT)
    //            .addAdditionalDataItem(new ApiTpsPolicyPair().key(GROUP).value(DDGROUP));
    //
    //    var inputs = new ApiTpsPolicyInputs().addInputsItem(groupPolicy);
    //
    //    var createRequest =
    //        new ApiTpsPaoCreateRequest()
    //            .component(ApiTpsComponent.valueOf("BPM"))
    //            .objectType(ApiTpsObjectType.valueOf("BILLING_PROFILE"))
    //            .objectId(UUID.fromString(("uuid")))
    //            //            .objectId(UUID.fromString(parameters.get("uuid").toString()))
    //            .attributes(inputs);
    //
    //    // Create a PAO
    //    tpsApiController.createPao(createRequest);
  }

  @State({"workspacePolicyExists"})
  public void workspacePolicyExists(Map<?, ?> parameters) throws Exception {

    //    // create a policy limited by workspace
    //    var workspacePolicy =
    //        new ApiTpsPolicyInput()
    //            .namespace(TERRA)
    //            .name(WORKSPACE_CONSTRAINT)
    //            .addAdditionalDataItem(new ApiTpsPolicyPair().key(WORKSPACE).value(WORKSPACE_ID));
    //
    //    var inputs = new ApiTpsPolicyInputs().addInputsItem(workspacePolicy);

    //     Create a PAO
    //    UUID groupPao = mvcUtils.createPao(inputs);
    //    String lastUpdated = checkInitialLastUpdate(paoIdA);
    //

  }

  @State({"regionPolicyExists"})
  public void regionPolicyExists() throws Exception {
    // create a policy limited by region
    //    var regionPolicy =
    //        new ApiTpsPolicyInput()
    //            .namespace(TERRA)
    //            .name(REGION_CONSTRAINT)
    //            .addAdditionalDataItem(new ApiTpsPolicyPair().key(REGION).value(US_REGION));
    //
    //    var inputs = new ApiTpsPolicyInputs().addInputsItem(regionPolicy);

    // Create a PAO
    //    UUID paoIdA = mvcUtils.createPao(inputs);
  }
}
