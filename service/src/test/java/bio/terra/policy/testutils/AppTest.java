package bio.terra.policy.testutils;

import bio.terra.policy.app.App;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/** Intended for tests that want the whole Spring application context */
@SpringBootTest(classes = App.class)
@AutoConfigureMockMvc
public class AppTest extends BaseTest {}
