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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TpsExplainControllerTest extends TestUnitBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String GROUP = "group";
  // TODO: PF-2503 - set these group names to different values when we can merge different groups.
  private static final String DDGROUP = "ddgroup";
  private static final String MNGROUP = DDGROUP;
  private static final String YUGROUP = DDGROUP;
  private static final String MCGROUP = DDGROUP;

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

  @Autowired private MvcUtils mvcUtils;

  /* TODO: PF-2321 Add tests for:
   *  - group and region policies
   *  - region conflict state
   */

  @Test
  public void explainEmptyPolicy() throws Exception {
    UUID emptyPao = mvcUtils.createEmptyPao();
    ApiTpsPaoExplainResult explainResult = mvcUtils.explainPao(emptyPao, 0);
    assertEquals(0, explainResult.getDepth());
    assertEquals(emptyPao, explainResult.getObjectId());
    checkExplainSources(explainResult, 1, emptyPao);
    assertTrue(explainResult.getExplanation().isEmpty());
  }

  @Test
  public void explainOnePolicyOnPao() throws Exception {
    UUID onePao = mvcUtils.createPao(DD_POLICY_INPUT);
    ApiTpsPaoExplainResult explainResult = mvcUtils.explainPao(onePao, 0);
    assertEquals(0, explainResult.getDepth());
    assertEquals(onePao, explainResult.getObjectId());
    checkExplainSources(explainResult, 1, onePao);

    ApiTpsPolicyExplanation explanation = getOnlyExplanation(explainResult);
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
    checkExplainSources(explainResult, 2, topPao, basePao);

    ApiTpsPolicyExplanation explanation = getOnlyExplanation(explainResult);
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
    checkExplainSources(explainResult, 2, topPao, basePao);

    ApiTpsPolicyExplanation explanation = getOnlyExplanation(explainResult);
    assertEquals(topPao, explanation.getObjectId());
    // Effective policy should be the merge of the two
    assertThat(
        explanation.getPolicyInput().getAdditionalData(), containsInAnyOrder(MN_POLICY_PAIR));

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

  /*
  topPao: set: none, eff: DD, MN, YU, MC
    - onePao: set: DD, eff: DD
    - twoPao: set: MN, eff MN, YU, MC
      - threePao: set: YU, eff: YU, MC
    -+- fourPao: set: MC, eff: MC (linked to both two pao and three pao)

    We delete twoPao and should see the delete flag show up in the sources.
   */
  @Test
  public void explainComplexPolicy() throws Exception {
    UUID topPao = mvcUtils.createEmptyPao();
    UUID onePao = mvcUtils.createPao(DD_POLICY_INPUT);
    UUID twoPao = mvcUtils.createPao(MN_POLICY_INPUT);
    UUID threePao = mvcUtils.createPao(YU_POLICY_INPUT);
    UUID fourPao = mvcUtils.createPao(MC_POLICY_INPUT);

    // Connect top to one and two
    ApiTpsPaoUpdateResult linkResult = mvcUtils.linkPao(topPao, onePao);
    assertTrue(linkResult.isUpdateApplied());
    linkResult = mvcUtils.linkPao(topPao, twoPao);
    assertTrue(linkResult.isUpdateApplied());

    // Connect two to three and four
    linkResult = mvcUtils.linkPao(twoPao, threePao);
    assertTrue(linkResult.isUpdateApplied());
    linkResult = mvcUtils.linkPao(twoPao, fourPao);
    assertTrue(linkResult.isUpdateApplied());

    // Connect three to four
    linkResult = mvcUtils.linkPao(threePao, fourPao);
    assertTrue(linkResult.isUpdateApplied());

    // -- check depth 1 --
    ApiTpsPaoExplainResult explainResult = mvcUtils.explainPao(topPao, 1);
    checkExplainSources(explainResult, 3, topPao, onePao, twoPao);

    ApiTpsPolicyExplanation explanation = getOnlyExplanation(explainResult);
    checkTop(explanation, topPao);

    // There should be two explanation under top and nothing under them
    assertEquals(2, explanation.getPolicyExplanations().size());
    for (ApiTpsPolicyExplanation exp : explanation.getPolicyExplanations()) {
      if (!checkOne(exp, onePao)) {
        if (!checkTwo(exp, twoPao)) {
          fail("invalid object id");
        }
      }
      assertEquals(0, exp.getPolicyExplanations().size());
    }

    // -- check depth 2 --
    explainResult = mvcUtils.explainPao(topPao, 2);
    checkExplainSources(explainResult, 5, topPao, onePao, twoPao, threePao, fourPao);
    explanation = getOnlyExplanation(explainResult);
    checkTop(explanation, topPao);

    // There should be two explanation under top
    assertEquals(2, explanation.getPolicyExplanations().size());
    for (ApiTpsPolicyExplanation exp : explanation.getPolicyExplanations()) {
      if (checkOne(exp, onePao)) {
        assertEquals(0, exp.getPolicyExplanations().size());
      } else if (checkTwo(exp, twoPao)) {
        // There should be three explanations under two: its own setting and effective
        // policies of three and four
        assertEquals(3, exp.getPolicyExplanations().size());
        for (ApiTpsPolicyExplanation expTwo : exp.getPolicyExplanations()) {
          if (!checkTwoSet(expTwo, twoPao)) {
            if (!checkThree(expTwo, threePao)) {
              if (!checkFour(expTwo, fourPao)) {
                fail("invalid object id");
              }
            }
          }
          assertEquals(0, expTwo.getPolicyExplanations().size());
        }
      }
    }

    // -- check depth 3 --
    explainResult = mvcUtils.explainPao(topPao, 3);
    checkExplainSources(explainResult, 5, topPao, onePao, twoPao, threePao, fourPao);
    explanation = getOnlyExplanation(explainResult);
    checkTop(explanation, topPao);

    // There should be two explanation under top
    assertEquals(2, explanation.getPolicyExplanations().size());
    for (ApiTpsPolicyExplanation exp : explanation.getPolicyExplanations()) {
      if (checkOne(exp, onePao)) {
        assertEquals(0, exp.getPolicyExplanations().size());
      } else if (checkTwo(exp, twoPao)) {
        // There should be three explanations under twoPao: its own setting and effective
        // policies of three and four
        assertEquals(3, exp.getPolicyExplanations().size());
        for (ApiTpsPolicyExplanation expTwo : exp.getPolicyExplanations()) {
          if (!checkTwoSet(expTwo, twoPao)) {
            if (!checkFour(expTwo, fourPao)) {
              if (!checkThree(expTwo, threePao)) {
                fail("invalid object id");
              } else {
                // There should be two explanations under three: its own setting and effective of
                // four
                assertEquals(2, expTwo.getPolicyExplanations().size());
                for (ApiTpsPolicyExplanation expThree : expTwo.getPolicyExplanations()) {
                  if (!checkThreeSet(expThree, threePao)) {
                    if (!checkFour(expThree, fourPao)) {
                      fail("invalid object id");
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // -- delete test --
    mvcUtils.deletePao(twoPao);
    explainResult = mvcUtils.explainPao(topPao, 1);
    List<ApiTpsPolicyExplainSource> sources = explainResult.getExplainObjects();
    assertEquals(3, sources.size());
    for (ApiTpsPolicyExplainSource source : sources) {
      if (source.getObjectId().equals(topPao)) {
        assertFalse(source.isDeleted());
      } else if (source.getObjectId().equals(onePao)) {
        assertFalse(source.isDeleted());
      } else if (source.getObjectId().equals(twoPao)) {
        assertTrue(source.isDeleted());
      } else {
        fail("invalid source");
      }
    }
  }

  // For the complex test, make checkers for each node to simplify the logic
  private void checkTop(ApiTpsPolicyExplanation exp, UUID objectId) {
    assertEquals(objectId, exp.getObjectId());
    checkPolicyInput(exp.getPolicyInput().getAdditionalData(), DDGROUP, MNGROUP, YUGROUP, MCGROUP);
  }

  private boolean checkOne(ApiTpsPolicyExplanation exp, UUID objectId) {
    if (exp.getObjectId().equals(objectId)) {
      assertEquals(DD_POLICY_INPUT, exp.getPolicyInput());
      return true;
    }
    return false;
  }

  private boolean checkTwo(ApiTpsPolicyExplanation exp, UUID objectId) {
    if (exp.getObjectId().equals(objectId)) {
      checkPolicyInput(exp.getPolicyInput().getAdditionalData(), MNGROUP, YUGROUP, MCGROUP);
      return true;
    }
    return false;
  }

  private boolean checkTwoSet(ApiTpsPolicyExplanation exp, UUID objectId) {
    if (exp.getObjectId().equals(objectId)) {
      assertEquals(MN_POLICY_INPUT, exp.getPolicyInput());
      return true;
    }
    return false;
  }

  private boolean checkThree(ApiTpsPolicyExplanation exp, UUID objectId) {
    if (exp.getObjectId().equals(objectId)) {
      checkPolicyInput(exp.getPolicyInput().getAdditionalData(), YUGROUP, MCGROUP);
      return true;
    }
    return false;
  }

  private boolean checkThreeSet(ApiTpsPolicyExplanation exp, UUID objectId) {
    if (exp.getObjectId().equals(objectId)) {
      assertEquals(YU_POLICY_INPUT, exp.getPolicyInput());
      return true;
    }
    return false;
  }

  private boolean checkFour(ApiTpsPolicyExplanation exp, UUID objectId) {
    if (exp.getObjectId().equals(objectId)) {
      assertEquals(MC_POLICY_INPUT, exp.getPolicyInput());
      return true;
    }
    return false;
  }

  // Small DRY for the case where we have one policy type (e.g., group) so should only have one
  // explanation
  private ApiTpsPolicyExplanation getOnlyExplanation(ApiTpsPaoExplainResult explainResult) {
    assertEquals(1, explainResult.getExplanation().size());
    return explainResult.getExplanation().get(0);
  }

  private void checkPolicyInput(List<ApiTpsPolicyPair> policyPairs, String... expectedGroups) {
    List<String> actualGroups = policyPairs.stream().map(ApiTpsPolicyPair::getValue).toList();
    Set<String> expectedSet = new HashSet<>(Arrays.asList(expectedGroups));
    assertThat(actualGroups, containsInAnyOrder(expectedSet.toArray()));
  }

  private void checkExplainSources(
      ApiTpsPaoExplainResult explainResult, int expectedCount, UUID... expectedSources) {
    assertEquals(expectedCount, explainResult.getExplainObjects().size());
    if (expectedCount > 0) {
      Map<UUID, ApiTpsPolicyExplainSource> explainSourceMap =
          makeExplainSourceMap(explainResult.getExplainObjects());
      for (UUID expectedSource : expectedSources) {
        ApiTpsPolicyExplainSource explainSource = explainSourceMap.get(expectedSource);
        assertNotNull(explainSource);
        assertEquals(ApiTpsComponent.WSM, explainSource.getComponent());
        assertEquals(ApiTpsObjectType.WORKSPACE, explainSource.getObjectType());
        assertEquals(expectedSource, explainSource.getObjectId());
        assertFalse(explainSource.isDeleted());
      }
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
