package bio.terra.policy.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.policy.app.controller.SwaggerController;
import bio.terra.policy.testutils.TestBase;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SwaggerController.class)
@WebMvcTest
class SwaggerControllerTest extends TestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void testGetSwagger() throws Exception {
    var swaggerPaths = Set.of("/", "/index.html");
    for (var path : swaggerPaths) {
      this.mockMvc
          .perform(get(path))
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("clientId"));
    }
  }
}
