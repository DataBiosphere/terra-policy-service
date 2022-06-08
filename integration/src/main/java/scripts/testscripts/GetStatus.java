package scripts.testscripts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import bio.terra.policy.api.PublicApi;
import bio.terra.policy.client.ApiClient;
import bio.terra.testrunner.runner.TestScript;
import bio.terra.testrunner.runner.config.TestUserSpecification;
import com.google.api.client.http.HttpStatusCodes;
import scripts.utils.ClientTestUtils;

public class GetStatus extends TestScript {
  @Override
  public void userJourney(TestUserSpecification testUser) throws Exception {
    ApiClient apiClient = ClientTestUtils.getClientWithoutAccessToken(server);
    var publicApi = new PublicApi(apiClient);
    publicApi.serviceStatus();
    assertThat(apiClient.getStatusCode(), is(HttpStatusCodes.STATUS_CODE_OK));
  }
}
