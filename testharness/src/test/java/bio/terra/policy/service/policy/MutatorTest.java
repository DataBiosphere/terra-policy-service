package bio.terra.policy.service.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.testutils.LibraryTestBase;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class MutatorTest extends LibraryTestBase {
  private static final String TERRA = "terra";
  private static final String GROUP_CONSTRAINT = "group-constraint";
  private static final String GROUP = "group";
  private static final String DDGROUP = "ddgroup";
  private static final String MCGROUP = "mcgroup";
  private static final String YUGROUP = "yugroup";

  private Multimap<String, String> buildMultimap(String key, String... values) {
    Multimap<String, String> mm = ArrayListMultimap.create();
    for (String value : values) {
      mm.put(key, value);
    }
    return mm;
  }

  @Test
  void groupConstraintTest() throws Exception {
    var policyGroup = new PolicyGroupConstraint();

    var ddgroupPolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP, DDGROUP));
    var mcgroupPolicy = new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP, MCGROUP));
    var mcyugroupPolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP, MCGROUP, YUGROUP));
    var mcyuddgroupPolicy =
        new PolicyInput(TERRA, GROUP_CONSTRAINT, buildMultimap(GROUP, MCGROUP, YUGROUP, DDGROUP));

    PolicyInput ddmc = policyGroup.combine(ddgroupPolicy, mcgroupPolicy);

    // Simple case
    Set<String> groupSet = policyGroup.dataToSet(ddmc.getAdditionalData().get(GROUP));
    assertEquals(2, groupSet.size(), "Contains 2 groups");
    assertThat(groupSet, containsInAnyOrder(DDGROUP, MCGROUP));

    // Multiple case
    PolicyInput ddmcyu = policyGroup.combine(ddgroupPolicy, mcyugroupPolicy);
    groupSet = policyGroup.dataToSet(ddmcyu.getAdditionalData().get(GROUP));
    assertThat(groupSet, containsInAnyOrder(DDGROUP, MCGROUP, YUGROUP));

    // Make sure order is not important
    PolicyInput mcyudd = policyGroup.combine(mcyugroupPolicy, ddgroupPolicy);
    groupSet = policyGroup.dataToSet(mcyudd.getAdditionalData().get(GROUP));
    assertThat(groupSet, containsInAnyOrder(DDGROUP, MCGROUP, YUGROUP));

    // remove dd from a group of 3
    PolicyInput nodd = policyGroup.remove(mcyuddgroupPolicy, ddgroupPolicy);
    groupSet = policyGroup.dataToSet(nodd.getAdditionalData().get(GROUP));
    assertThat(groupSet, containsInAnyOrder(MCGROUP, YUGROUP));

    // remove dd when dd is not there to start with
    nodd = policyGroup.remove(mcyugroupPolicy, ddgroupPolicy);
    groupSet = policyGroup.dataToSet(nodd.getAdditionalData().get(GROUP));
    assertThat(groupSet, containsInAnyOrder(MCGROUP, YUGROUP));

    // remove dd when dd is the only group there
    nodd = policyGroup.remove(ddgroupPolicy, ddgroupPolicy);
    assertNull(nodd);
  }
}
