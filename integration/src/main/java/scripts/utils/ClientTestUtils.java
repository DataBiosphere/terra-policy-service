package scripts.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.api.PolicyApi;
import bio.terra.policy.client.ApiClient;
import bio.terra.testrunner.common.utils.AuthenticationUtils;
import bio.terra.testrunner.runner.config.ServerSpecification;
import bio.terra.testrunner.runner.config.TestUserSpecification;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.base.Strings;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientTestUtils {
  private static final Logger logger = LoggerFactory.getLogger(ClientTestUtils.class);

  // Required scopes for client tests include the usual login scopes and GCP scope.
  public static final List<String> TEST_USER_SCOPES =
      List.of("openid", "email", "profile", "https://www.googleapis.com/auth/cloud-platform");

  private ClientTestUtils() {}

  /**
   * Build the Workspace Manager Service API client object for the given server specification. It is
   * setting the access token for the Test Runner SA specified in the given server specification.
   *
   * <p>A Test Runner SA is a GCP SA with appropriate permissions / scopes to run all the client
   * tests within this repo. For example, to run resiliency tests against K8S infrastructure, you'll
   * need a SA powerful enough to do things like list, read, and update.
   *
   * @param server the server we are testing against
   * @return the API client object
   */
  public static ApiClient getClientForTestRunnerSA(ServerSpecification server) throws IOException {
    if (server.testRunnerServiceAccount == null) {
      throw new IllegalArgumentException(
          "Workspace Manager Service client service account is required");
    }

    // refresh the client service account token
    GoogleCredentials serviceAccountCredential =
        AuthenticationUtils.getServiceAccountCredential(
            server.testRunnerServiceAccount, AuthenticationUtils.userLoginScopes);
    AccessToken accessToken = AuthenticationUtils.getAccessToken(serviceAccountCredential);
    logger.debug(
        "Generated access token for workspace manager service client SA: {}",
        server.testRunnerServiceAccount.name);

    return buildClient(accessToken, server);
  }

  /**
   * Build the Workspace Manager API client object for the given test user and server
   * specifications. The test user's token is always refreshed
   *
   * @param testUser the test user whose credentials are supplied to the API client object
   * @param server the server we are testing against
   * @return the API client object for this user
   */
  public static ApiClient getClientForTestUser(
      TestUserSpecification testUser, ServerSpecification server) throws IOException {
    AccessToken accessToken = null;

    // if no test user is specified, then return a client object without an access token set
    // this is useful if the caller wants to make ONLY unauthenticated calls
    if (testUser != null) {
      logger.debug(
          "Fetching credentials and building Workspace Manager ApiClient object for test user: {}",
          testUser.name);
      GoogleCredentials userCredential =
          AuthenticationUtils.getDelegatedUserCredential(testUser, TEST_USER_SCOPES);
      accessToken = AuthenticationUtils.getAccessToken(userCredential);
    }

    return buildClient(accessToken, server);
  }

  public static PolicyApi getPolicyClient(
      TestUserSpecification testUser, ServerSpecification server) throws IOException {
    final ApiClient apiClient = getClientForTestUser(testUser, server);
    return new PolicyApi(apiClient);
  }

  public static PolicyApi getPolicyClientFromToken(
      AccessToken accessToken, ServerSpecification server) throws IOException {
    final ApiClient apiClient = buildClient(accessToken, server);
    return new PolicyApi(apiClient);
  }

  /**
   * Build the Policy API client object for the server specifications. No access token is needed for
   * this API client.
   *
   * @param server the server we are testing against
   * @return the API client object for this user
   */
  public static ApiClient getClientWithoutAccessToken(ServerSpecification server)
      throws IOException {
    return buildClient(null, server);
  }

  // TODO: Need to add policyServiceUri to the ServerSpecification in TestRunner.
  //  Although that seems crazy to have to update TestRunner for every component.
  private static ApiClient buildClient(
      @Nullable AccessToken accessToken, ServerSpecification server) throws IOException {
    if (Strings.isNullOrEmpty(server.workspaceManagerUri)) {
      throw new IllegalArgumentException("Policy Service URI cannot be empty");
    }

    // build the client object
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(server.workspaceManagerUri);

    if (accessToken != null) {
      apiClient.setAccessToken(accessToken.getTokenValue());
    }

    return apiClient;
  }

  public static void assertHttpSuccess(PolicyApi workspaceApi, String label) {
    int httpCode = workspaceApi.getApiClient().getStatusCode();
    logger.debug("{} HTTP code: {}", label, httpCode);
    assertThat(HttpStatusCodes.isSuccess(httpCode), equalTo(true));
  }

  @FunctionalInterface
  public interface SupplierWithException<T> {
    T get() throws Exception;
  }

  /**
   * Get a result from a call that might throw an exception. Treat the exception as retryable, sleep
   * for 15 seconds, and retry up to 40 times. This structure is useful for situations where we are
   * waiting on a cloud IAM permission change to take effect.
   *
   * @param supplier - code returning the result or throwing an exception
   * @param <T> - type of result
   * @return - result from supplier, the first time it doesn't throw, or null if all tries have been
   *     exhausted
   * @throws InterruptedException if the sleep is interrupted
   */
  public static @Nullable <T> T getWithRetryOnException(SupplierWithException<T> supplier)
      throws Exception {
    T result = null;
    int numTries = 40;
    Duration sleepDuration = Duration.ofSeconds(15);
    while (numTries > 0) {
      try {
        result = supplier.get();
        break;
      } catch (Exception e) {
        numTries--;
        if (0 == numTries) {
          throw e;
        }
        logger.info(
            "Exception \"{}\". Waiting {} seconds for permissions to propagate. Tries remaining: {}",
            e.getMessage(),
            sleepDuration.toSeconds(),
            numTries);
        TimeUnit.MILLISECONDS.sleep(sleepDuration.toMillis());
      }
    }
    return result;
  }

  public static void runWithRetryOnException(Runnable fn) throws Exception {
    getWithRetryOnException(
        () -> {
          fn.run();
          return null;
        });
  }

  /**
   * Check Optional's value is present and return it, or else fail an assertion.
   *
   * @param optional - Optional expression
   * @param <T> - value type of optional
   * @return - value of optional, if present
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static <T> T assertPresent(Optional<T> optional, @Nullable String message) {
    assertTrue(
        optional.isPresent(), Optional.ofNullable(message).orElse("Optional value not present."));
    return optional.get();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static <T> T assertPresent(Optional<T> optional) {
    return assertPresent(optional, null);
  }
}
