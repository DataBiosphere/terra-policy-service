package bio.terra.policy.controller;

import static bio.terra.policy.testutils.MockMvcUtils.addAuth;
import static bio.terra.policy.testutils.MockMvcUtils.addJsonContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import bio.terra.policy.generated.model.ApiTpsComponent;
import bio.terra.policy.generated.model.ApiTpsObjectType;
import bio.terra.policy.generated.model.ApiTpsPaoCreateRequest;
import bio.terra.policy.generated.model.ApiTpsPaoExplainResult;
import bio.terra.policy.generated.model.ApiTpsPaoGetResult;
import bio.terra.policy.generated.model.ApiTpsPaoReplaceRequest;
import bio.terra.policy.generated.model.ApiTpsPaoSourceRequest;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateRequest;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateResult;
import bio.terra.policy.generated.model.ApiTpsPolicyInput;
import bio.terra.policy.generated.model.ApiTpsPolicyInputs;
import bio.terra.policy.generated.model.ApiTpsUpdateMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Component
public class MvcUtils {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  /**
   * Create a PAO from policy inputs
   *
   * @param inputs set of policy inputs
   * @return UUID of the PAO
   */
  public UUID createPao(ApiTpsPolicyInputs inputs) throws Exception {
    UUID objectId = UUID.randomUUID();
    var apiRequest =
        new ApiTpsPaoCreateRequest()
            .component(ApiTpsComponent.WSM)
            .objectType(ApiTpsObjectType.WORKSPACE)
            .objectId(objectId)
            .attributes(inputs);

    String json = objectMapper.writeValueAsString(apiRequest);

    MvcResult result =
        mockMvc
            .perform(addAuth(addJsonContentType(post("/api/policy/v1alpha1/pao").content(json))))
            .andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.NO_CONTENT, status);

    return objectId;
  }

  /**
   * Create a PAO with a single policy input
   *
   * @param input one policy input
   * @return UUID of PAO
   */
  public UUID createPao(ApiTpsPolicyInput input) throws Exception {
    return createPao(new ApiTpsPolicyInputs().addInputsItem(input));
  }

  public void deletePao(UUID objectId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(addAuth(addJsonContentType(delete("/api/policy/v1alpha1/pao/" + objectId))))
            .andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.NO_CONTENT, status);
  }

  /**
   * Convenience method for the common case of making an empty PAO
   *
   * @return UUID of pao
   */
  public UUID createEmptyPao() throws Exception {
    return createPao(new ApiTpsPolicyInputs());
  }

  public ApiTpsPaoExplainResult explainPao(UUID objectId, Integer depth) throws Exception {
    String path = "/api/policy/v1alpha1/pao/" + objectId + "/explain";
    if (depth != null) {
      path = path + "?depth=" + depth;
    }

    MvcResult result = mockMvc.perform(addAuth(addJsonContentType(get(path)))).andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);

    return objectMapper.readValue(response.getContentAsString(), ApiTpsPaoExplainResult.class);
  }

  public ApiTpsPaoGetResult getPao(UUID objectId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(addAuth(addJsonContentType(get("/api/policy/v1alpha1/pao/" + objectId))))
            .andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);
    var apiPao = objectMapper.readValue(response.getContentAsString(), ApiTpsPaoGetResult.class);
    assertEquals(objectId, apiPao.getObjectId());
    assertEquals(ApiTpsComponent.WSM, apiPao.getComponent());
    assertEquals(ApiTpsObjectType.WORKSPACE, apiPao.getObjectType());
    return apiPao;
  }

  public ApiTpsPaoUpdateResult linkPao(UUID dependentId, UUID sourceId) throws Exception {
    return connectPao(dependentId, sourceId, "link", ApiTpsUpdateMode.FAIL_ON_CONFLICT);
  }

  public ApiTpsPaoUpdateResult mergePao(UUID dependentId, UUID sourceId) throws Exception {
    return connectPao(dependentId, sourceId, "merge", ApiTpsUpdateMode.FAIL_ON_CONFLICT);
  }

  public ApiTpsPaoUpdateResult replacePao(UUID objectId, ApiTpsPolicyInputs inputs)
      throws Exception {
    var replaceRequest =
        new ApiTpsPaoReplaceRequest()
            .updateMode(ApiTpsUpdateMode.FAIL_ON_CONFLICT)
            .newAttributes(inputs);
    var replaceJson = objectMapper.writeValueAsString(replaceRequest);
    MvcResult result =
        mockMvc
            .perform(
                addAuth(
                    addJsonContentType(
                        put("/api/policy/v1alpha1/pao/" + objectId).content(replaceJson))))
            .andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);
    return objectMapper.readValue(response.getContentAsString(), ApiTpsPaoUpdateResult.class);
  }

  public ApiTpsPaoUpdateResult updatePao(UUID objectId, ApiTpsPolicyInputs inputs)
      throws Exception {
    var updateRequest =
        new ApiTpsPaoUpdateRequest()
            .updateMode(ApiTpsUpdateMode.FAIL_ON_CONFLICT)
            .addAttributes(inputs);
    var updateJson = objectMapper.writeValueAsString(updateRequest);
    MvcResult result =
        mockMvc
            .perform(
                addAuth(
                    addJsonContentType(
                        patch("/api/policy/v1alpha1/pao/" + objectId).content(updateJson))))
            .andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);
    return objectMapper.readValue(response.getContentAsString(), ApiTpsPaoUpdateResult.class);
  }

  private ApiTpsPaoUpdateResult connectPao(
      UUID dependentId, UUID sourceId, String operation, ApiTpsUpdateMode updateMode)
      throws Exception {
    var connectRequest =
        new ApiTpsPaoSourceRequest().sourceObjectId(sourceId).updateMode(updateMode);
    String connectJson = objectMapper.writeValueAsString(connectRequest);
    String url = String.format("/api/policy/v1alpha1/pao/%s/%s", dependentId, operation);

    MvcResult result =
        mockMvc.perform(addAuth(addJsonContentType(post(url).content(connectJson)))).andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);
    return objectMapper.readValue(response.getContentAsString(), ApiTpsPaoUpdateResult.class);
  }
}
