package bio.terra.policy.controller;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import bio.terra.policy.app.controller.PublicApiController;
import bio.terra.policy.app.controller.TpsApiController;
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
@ContextConfiguration(classes = {PublicApiController.class})
@Provider("tps")
@PactBroker()
class VerifyPactsTpsPublicApiController {

  @MockBean private PublicApiController publicApiController;
  @MockBean private TpsApiController tpsApiController;

  // This mockMVC is what we use to test API requests and responses:
  @Autowired private MockMvc mockMvc;

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(new MockMvcTestTarget(mockMvc));
  }

  @State({"Tps is ok"})
  public void checkStatus() throws Exception {}
}
