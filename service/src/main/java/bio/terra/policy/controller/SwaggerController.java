package bio.terra.policy.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerController {

  private String swaggerClientId;

  public SwaggerController() {
    try {
      try (var reader =
          new BufferedReader(
              new InputStreamReader(
                  new ClassPathResource("rendered/swagger-client-id").getInputStream(),
                  StandardCharsets.UTF_8))) {
        swaggerClientId = reader.readLine();
      }
    } catch (IOException e) {
      swaggerClientId = "";
    }
  }

  @GetMapping({"/", "/index.html", "swagger-ui.html"})
  public String getSwagger(Model model) {
    model.addAttribute("clientId", swaggerClientId);
    return "swagger-ui";
  }
}
