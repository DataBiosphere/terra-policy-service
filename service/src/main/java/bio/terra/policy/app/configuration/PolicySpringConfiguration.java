package bio.terra.policy.app.configuration;

import bio.terra.policy.app.StartupInitializer;
import bio.terra.policy.generated.model.ApiVersionProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PolicySpringConfiguration {
  @Bean
  @ConfigurationProperties("tps.version")
  public ApiVersionProperties getTpsVersion() {
    return new ApiVersionProperties();
  }

  @Bean("objectMapper")
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper =
        new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            // Disable serialization Date as Timestamp so swagger shows date in the RFC3339
            // format, see details in PF-1855.
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);
    objectMapper.getFactory().setCharacterEscapes(new HTMLCharacterEscapes());
    return objectMapper;
  }

  // This is a "magic bean": It supplies a method that Spring calls after the application is setup,
  // but before the port is opened for business. That lets us do database migration and stairway
  // initialization on a system that is otherwise fully configured. The rule of thumb is that all
  // bean initialization should avoid database access. If there is additional database work to be
  // done, it should happen inside this method.
  @Bean
  public SmartInitializingSingleton postSetupInitialization(ApplicationContext applicationContext) {
    return () -> StartupInitializer.initialize(applicationContext);
  }

  // Took this from WSM. It should probably be in TCL
  public static class HTMLCharacterEscapes extends CharacterEscapes {
    private static final int[] asciiEscapes;

    static {
      // Start with a copy of the default set of escaped characters, then modify.
      asciiEscapes = CharacterEscapes.standardAsciiEscapesForJSON();
      // Escape HTML metacharacters for security reasons. For JSON, CharacterEscapes.ESCAPE_STANDARD
      // means unicode-escaping.
      asciiEscapes['<'] = CharacterEscapes.ESCAPE_STANDARD;
      asciiEscapes['>'] = CharacterEscapes.ESCAPE_STANDARD;
      asciiEscapes['&'] = CharacterEscapes.ESCAPE_STANDARD;
    }

    /** Return the escape codes used for ASCII characters. */
    @Override
    @SuppressFBWarnings(
        value = "EI",
        justification = "per base class documentation, callers may not modify returned value")
    public int[] getEscapeCodesForAscii() {
      return asciiEscapes;
    }

    /**
     * Return the escape codes used for non-ASCII characters, or ASCII characters with escape code
     * set to ESCAPE_CUSTOM.
     *
     * <p>This is required because CharacterEscapes is abstract. We don't need additional escaping
     * for any characters, so per documentation this can return null for all inputs.
     */
    @Override
    public SerializableString getEscapeSequence(int ch) {
      return null;
    }
  }
}
