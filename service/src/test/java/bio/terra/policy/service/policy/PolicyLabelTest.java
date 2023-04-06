package bio.terra.policy.service.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.testutils.TestUnitBase;
import com.google.common.collect.ArrayListMultimap;
import org.junit.jupiter.api.Test;

public class PolicyLabelTest extends TestUnitBase {
  private final PolicyName testName = new PolicyName("terra", "test-policy");
  private final PolicyInput testPolicyInput = new PolicyInput(testName, ArrayListMultimap.create());

  @Test
  void testCombine() {
    assertEquals(
        testName, new PolicyLabel(testName).combine(testPolicyInput, null).getPolicyName());
    assertEquals(
        testName, new PolicyLabel(testName).combine(null, testPolicyInput).getPolicyName());
    assertNull(new PolicyLabel(testName).combine(null, null));
  }

  @Test
  void testRemove() {
    assertNull(new PolicyLabel(testName).remove(testPolicyInput, testPolicyInput));
  }

  @Test
  void testValidate_valid() {
    assertTrue(new PolicyLabel(testName).isValid(testPolicyInput));
  }

  @Test
  void testValidate_invalid() {
    final ArrayListMultimap<String, String> additionalData = ArrayListMultimap.create();
    additionalData.put("foo", "bar");
    assertFalse(new PolicyLabel(testName).isValid(new PolicyInput(testName, additionalData)));
  }
}
