package bio.terra.policy.testutils;

import bio.terra.policy.app.PolicySpringApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Tag("library")
@ActiveProfiles({"library-test", "human-readable-logging"})
@ContextConfiguration(classes = PolicySpringApplication.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest()
public class LibraryTestBase {}
