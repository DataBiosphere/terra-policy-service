package bio.terra.policy.testutils;

import bio.terra.policy.app.PolicySpringApplication;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles({"test", "human-readable-logging"})
@ContextConfiguration(classes = PolicySpringApplication.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest()
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
public class TestUnitBase {}
