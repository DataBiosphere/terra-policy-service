package bio.terra.policy.controller;

import static bio.terra.policy.testutils.MockMvcUtils.addAuth;
import static bio.terra.policy.testutils.MockMvcUtils.addJsonContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import bio.terra.policy.generated.model.ApiTpsComponent;
import bio.terra.policy.generated.model.ApiTpsObjectType;
import bio.terra.policy.generated.model.ApiTpsPaoCreateRequest;
import bio.terra.policy.generated.model.ApiTpsPaoGetResult;
import bio.terra.policy.generated.model.ApiTpsPaoReplaceRequest;
import bio.terra.policy.generated.model.ApiTpsPaoSourceRequest;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateRequest;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateResult;
import bio.terra.policy.generated.model.ApiTpsPolicyInput;
import bio.terra.policy.generated.model.ApiTpsPolicyInputs;
import bio.terra.policy.generated.model.ApiTpsPolicyPair;
import bio.terra.policy.generated.model.ApiTpsUpdateMode;
import bio.terra.policy.testutils.TestUnitBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
public class TpsBasicControllerTest extends TestUnitBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String REGION_CONSTRAINT = "region-constraint";
  private static final String GROUP = "group";
  private static final String REGION = "region";
  private static final String DDGROUP = "ddgroup";
  private static final String US_REGION = "US";

  @Autowired private ObjectMapper objectMapper;
  @Autowired private MockMvc mockMvc;

  @Test
  public void basicPaoTest() throws Exception {
    var groupPolicy =
        new ApiTpsPolicyInput()
            .namespace(TERRA)
            .name(GROUP_CONSTRAINT)
            .addAdditionalDataItem(new ApiTpsPolicyPair().key(GROUP).value(DDGROUP));

    var regionPolicy =
        new ApiTpsPolicyInput()
            .namespace(TERRA)
            .name(REGION_CONSTRAINT)
            .addAdditionalDataItem(new ApiTpsPolicyPair().key(REGION).value(US_REGION));

    var inputs = new ApiTpsPolicyInputs().addInputsItem(groupPolicy).addInputsItem(regionPolicy);

    // Create a PAO
    UUID paoIdA = createPao(inputs);

    // Create another PAO with no policies
    UUID paoIdB = createPao(new ApiTpsPolicyInputs());

    // Get a PAO
    MvcResult result =
        mockMvc
            .perform(addAuth(addJsonContentType(get("/api/policy/v1alpha1/pao/" + paoIdA))))
            .andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);

    var apiPao = objectMapper.readValue(response.getContentAsString(), ApiTpsPaoGetResult.class);
    assertEquals(paoIdA, apiPao.getObjectId());
    assertEquals(ApiTpsComponent.WSM, apiPao.getComponent());
    assertEquals(ApiTpsObjectType.WORKSPACE, apiPao.getObjectType());
    checkAttributeSet(apiPao.getAttributes());
    checkAttributeSet(apiPao.getEffectiveAttributes());

    // Merge a PAO
    var updateResult = connectPao(paoIdB, paoIdA, "merge");
    assertTrue(updateResult.isUpdateApplied());
    assertEquals(0, updateResult.getConflicts().size());
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes());

    // Link a PAO
    updateResult = connectPao(paoIdB, paoIdA, "link");
    assertTrue(updateResult.isUpdateApplied());
    assertEquals(0, updateResult.getConflicts().size());
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes());

    // Update a PAO
    var updateRequest =
        new ApiTpsPaoUpdateRequest()
            .updateMode(ApiTpsUpdateMode.FAIL_ON_CONFLICT)
            .addAttributes(inputs);
    var updateJson = objectMapper.writeValueAsString(updateRequest);

    result =
        mockMvc
            .perform(
                addAuth(
                    addJsonContentType(
                        patch("/api/policy/v1alpha1/pao/" + paoIdB).content(updateJson))))
            .andReturn();
    response = result.getResponse();
    status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);
    updateResult =
        objectMapper.readValue(response.getContentAsString(), ApiTpsPaoUpdateResult.class);
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes());

    // Replace a PAO
    var replaceRequest =
        new ApiTpsPaoReplaceRequest()
            .updateMode(ApiTpsUpdateMode.FAIL_ON_CONFLICT)
            .newAttributes(inputs);
    var replaceJson = objectMapper.writeValueAsString(replaceRequest);
    result =
        mockMvc
            .perform(
                addAuth(
                    addJsonContentType(
                        put("/api/policy/v1alpha1/pao/" + paoIdB).content(replaceJson))))
            .andReturn();
    response = result.getResponse();
    status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);
    updateResult =
        objectMapper.readValue(response.getContentAsString(), ApiTpsPaoUpdateResult.class);
    checkAttributeSet(updateResult.getResultingPao().getEffectiveAttributes());

    // Delete a PAO
    deletePao(paoIdA);
    deletePao(paoIdB);
  }

  private void checkAttributeSet(ApiTpsPolicyInputs attributeSet) {
    for (ApiTpsPolicyInput attribute : attributeSet.getInputs()) {
      assertEquals(TERRA, attribute.getNamespace());
      assertEquals(1, attribute.getAdditionalData().size());

      if (attribute.getName().equals(GROUP_CONSTRAINT)) {
        assertEquals(GROUP, attribute.getAdditionalData().get(0).getKey());
        assertEquals(DDGROUP, attribute.getAdditionalData().get(0).getValue());
      } else if (attribute.getName().equals(REGION_CONSTRAINT)) {
        assertEquals(REGION, attribute.getAdditionalData().get(0).getKey());
        assertEquals(US_REGION, attribute.getAdditionalData().get(0).getValue());
      } else {
        fail();
      }
    }
  }

  private UUID createPao(ApiTpsPolicyInputs inputs) throws Exception {
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

  private ApiTpsPaoUpdateResult connectPao(UUID targetId, UUID sourceId, String operation)
      throws Exception {
    var connectRequest =
        new ApiTpsPaoSourceRequest()
            .sourceObjectId(sourceId)
            .updateMode(ApiTpsUpdateMode.FAIL_ON_CONFLICT);
    String connectJson = objectMapper.writeValueAsString(connectRequest);
    String url = String.format("/api/policy/v1alpha1/pao/%s/%s", targetId, operation);

    MvcResult result =
        mockMvc.perform(addAuth(addJsonContentType(post(url).content(connectJson)))).andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.OK, status);

    return objectMapper.readValue(response.getContentAsString(), ApiTpsPaoUpdateResult.class);
  }

  private void deletePao(UUID objectId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(addAuth(addJsonContentType(delete("/api/policy/v1alpha1/pao/" + objectId))))
            .andReturn();
    MockHttpServletResponse response = result.getResponse();
    HttpStatus status = HttpStatus.valueOf(response.getStatus());
    assertEquals(HttpStatus.NO_CONTENT, status);
  }
}