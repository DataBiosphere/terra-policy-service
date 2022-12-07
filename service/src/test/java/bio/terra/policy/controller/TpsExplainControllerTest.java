package bio.terra.policy.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import bio.terra.policy.generated.model.ApiTpsComponent;
import bio.terra.policy.generated.model.ApiTpsObjectType;
import bio.terra.policy.generated.model.ApiTpsPaoExplainResult;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateResult;
import bio.terra.policy.generated.model.ApiTpsPolicyExplainSource;
import bio.terra.policy.generated.model.ApiTpsPolicyExplanation;
import bio.terra.policy.generated.model.ApiTpsPolicyInput;
import bio.terra.policy.generated.model.ApiTpsPolicyPair;
import bio.terra.policy.testutils.TestUnitBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
public class TpsExplainControllerTest extends TestUnitBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String GROUP = "group";
  private static final String DDGROUP = "ddgroup";
  private static final String MNGROUP = "mngroup";
  private static final String YUGROUP = "yugroup";
  private static final String MCGROUP = "mcgroup";

  private static final ApiTpsPolicyPair DD_POLICY_PAIR =
      new ApiTpsPolicyPair().key(GROUP).value(DDGROUP);
  private static final ApiTpsPolicyInput DD_POLICY_INPUT =
      new ApiTpsPolicyInput()
          .namespace(TERRA)
          .name(GROUP_CONSTRAINT)
          .addAdditionalDataItem(DD_POLICY_PAIR);

  private static final ApiTpsPolicyPair MN_POLICY_PAIR =
      new ApiTpsPolicyPair().key(GROUP).value(MNGROUP);
  private static final ApiTpsPolicyInput MN_POLICY_INPUT =
      new ApiTpsPolicyInput()
          .namespace(TERRA)
          .name(GROUP_CONSTRAINT)
          .addAdditionalDataItem(MN_POLICY_PAIR);

  private static final ApiTpsPolicyPair YU_POLICY_PAIR =
      new ApiTpsPolicyPair().key(GROUP).value(YUGROUP);
  private static final ApiTpsPolicyInput YU_POLICY_INPUT =
      new ApiTpsPolicyInput()
          .namespace(TERRA)
          .name(GROUP_CONSTRAINT)
          .addAdditionalDataItem(YU_POLICY_PAIR);

  private static final ApiTpsPolicyPair MC_POLICY_PAIR =
      new ApiTpsPolicyPair().key(GROUP).value(MCGROUP);
  private static final ApiTpsPolicyInput MC_POLICY_INPUT =
      new ApiTpsPolicyInput()
          .namespace(TERRA)
          .name(GROUP_CONSTRAINT)
          .addAdditionalDataItem(MC_POLICY_PAIR);

  @Autowired private ObjectMapper objectMapper;
  @Autowired private MockMvc mockMvc;
  @Autowired private MvcUtils mvcUtils;

  /*
  Test cases:
  - policy on pao and on pao source
  - policy on pao and on pao source
  - complex graph
    - make sure it works!
    - depth cases 0, 1, 2
    - isDeleted
  - ones to do later:
    - group and region policies
    - region conflict state
   */

  @Test
  public void explainEmptyPolicy() throws Exception {
    UUID emptyPao = mvcUtils.createEmptyPao();
    ApiTpsPaoExplainResult explainResult = mvcUtils.explainPao(emptyPao, 0);
    assertEquals(0, explainResult.getDepth());
    assertEquals(emptyPao, explainResult.getObjectId());
    assertEquals(1, explainResult.getExplainObjects().size());
    assertTrue(explainResult.getExplanation().isEmpty());
  }

  @Test
  public void explainOnePolicyOnPao() throws Exception {
    UUID onePao = mvcUtils.createPao(DD_POLICY_INPUT);
    ApiTpsPaoExplainResult explainResult = mvcUtils.explainPao(onePao, 0);
    assertEquals(0, explainResult.getDepth());
    assertEquals(onePao, explainResult.getObjectId());
    assertEquals(1, explainResult.getExplainObjects().size());
    ApiTpsPolicyExplainSource explainSource = explainResult.getExplainObjects().get(0);
    assertEquals(ApiTpsComponent.WSM, explainSource.getComponent());
    assertEquals(ApiTpsObjectType.WORKSPACE, explainSource.getObjectType());
    assertEquals(onePao, explainSource.getObjectId());
    assertFalse(explainSource.isDeleted());
    assertEquals(1, explainResult.getExplanation().size());
    ApiTpsPolicyExplanation explanation = explainResult.getExplanation().get(0);
    assertEquals(onePao, explanation.getObjectId());
    assertEquals(DD_POLICY_INPUT, explanation.getPolicyInput());
    assertTrue(explanation.getPolicyExplanations().isEmpty());
  }

  @Test
  public void explainOneInheritedPolicy() throws Exception {
    // Setup base --> top
    UUID basePao = mvcUtils.createPao(MN_POLICY_INPUT);
    UUID topPao = mvcUtils.createEmptyPao();
    ApiTpsPaoUpdateResult linkResult = mvcUtils.linkPao(topPao, basePao);
    assertTrue(linkResult.isUpdateApplied());

    // Explain it to me
    ApiTpsPaoExplainResult explainResult = mvcUtils.explainPao(topPao, 0);
    assertEquals(0, explainResult.getDepth());
    assertEquals(topPao, explainResult.getObjectId());
    assertEquals(2, explainResult.getExplainObjects().size());
    Map<UUID, ApiTpsPolicyExplainSource> explainSourceMap =
        makeExplainSourceMap(explainResult.getExplainObjects());
    assertNotNull(explainSourceMap.get(topPao));
    assertNotNull(explainSourceMap.get(basePao));

    // One policy type (group) so should only have one explanation
    assertEquals(1, explainResult.getExplanation().size());
    ApiTpsPolicyExplanation explanation = explainResult.getExplanation().get(0);
    assertEquals(topPao, explanation.getObjectId());
    assertEquals(MN_POLICY_INPUT, explanation.getPolicyInput()); // essentially the effective policy

    // There should be one explanation under top
    assertEquals(1, explanation.getPolicyExplanations().size());
    explanation = explanation.getPolicyExplanations().get(0);
    assertEquals(basePao, explanation.getObjectId());
    assertEquals(MN_POLICY_INPUT, explanation.getPolicyInput()); // the set policy on base

    // There should be no explanations under base
    assertEquals(0, explanation.getPolicyExplanations().size());
  }

  @Test
  public void explainInheritedAndSetPolicy() throws Exception {
    // Setup base with group --> top with different group
    UUID basePao = mvcUtils.createPao(MN_POLICY_INPUT);
    UUID topPao = mvcUtils.createPao(YU_POLICY_INPUT);
    ApiTpsPaoUpdateResult linkResult = mvcUtils.linkPao(topPao, basePao);
    assertTrue(linkResult.isUpdateApplied());

    // Explain it to me
    ApiTpsPaoExplainResult explainResult = mvcUtils.explainPao(topPao, 0);
    assertEquals(0, explainResult.getDepth());
    assertEquals(topPao, explainResult.getObjectId());
    assertEquals(2, explainResult.getExplainObjects().size());
    Map<UUID, ApiTpsPolicyExplainSource> explainSourceMap =
        makeExplainSourceMap(explainResult.getExplainObjects());
    assertNotNull(explainSourceMap.get(topPao));
    assertNotNull(explainSourceMap.get(basePao));

    // One policy type (group) so should only have one explanation
    assertEquals(1, explainResult.getExplanation().size());
    ApiTpsPolicyExplanation explanation = explainResult.getExplanation().get(0);
    assertEquals(topPao, explanation.getObjectId());
    // Effective policy should be the merge of the two
    assertThat(
        explanation.getPolicyInput().getAdditionalData(),
        containsInAnyOrder(MN_POLICY_PAIR, YU_POLICY_PAIR));

    // There should be two explanation under top
    assertEquals(2, explanation.getPolicyExplanations().size());
    for (ApiTpsPolicyExplanation exp : explanation.getPolicyExplanations()) {
      if (exp.getObjectId().equals(topPao)) {
        assertEquals(YU_POLICY_INPUT, exp.getPolicyInput()); // the set policy on top
      } else if (exp.getObjectId().equals(basePao)) {
        assertEquals(MN_POLICY_INPUT, exp.getPolicyInput()); // the set policy on base
      } else {
        fail("Invalid object id");
      }
      assertEquals(0, exp.getPolicyExplanations().size());
    }
  }

  private Map<UUID, ApiTpsPolicyExplainSource> makeExplainSourceMap(
      List<ApiTpsPolicyExplainSource> sources) {
    Map<UUID, ApiTpsPolicyExplainSource> explainSourceMap = new HashMap<>();
    for (ApiTpsPolicyExplainSource source : sources) {
      explainSourceMap.put(source.getObjectId(), source);
    }
    return explainSourceMap;
  }
}
