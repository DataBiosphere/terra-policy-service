package bio.terra.policy.app;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestHarnessBeanConfig {
  // This is a "magic bean": It supplies a method that Spring calls after the application is setup,
  // but before the port is opened for business. That lets us do database migration and stairway
  // initialization on a system that is otherwise fully configured. The rule of thumb is that all
  // bean initialization should avoid database access. If there is additional database work to be
  // done, it should happen inside this method.
  @Bean
  public SmartInitializingSingleton postSetupInitialization(ApplicationContext applicationContext) {
    return () -> StartupInitializer.initialize(applicationContext);
  }
}
