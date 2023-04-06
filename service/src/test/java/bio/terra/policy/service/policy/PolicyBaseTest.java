package bio.terra.policy.service.policy;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.policy.common.exception.InternalTpsErrorException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.testutils.TestUnitBase;
import com.google.common.collect.ArrayListMultimap;
import org.junit.jupiter.api.Test;

public class PolicyBaseTest extends TestUnitBase {
  private final PolicyName testName1 = new PolicyName("terra", "test-policy1");
  private final PolicyName testName2 = new PolicyName("terra", "test-policy2");
  private final PolicyInput testPolicyInput =
      new PolicyInput(testName2, ArrayListMultimap.create());

  private final PolicyBase testPolicy =
      new PolicyBase() {
        @Override
        public PolicyName getPolicyName() {
          return testName1;
        }

        @Override
        protected PolicyInput performCombine(PolicyInput dependent, PolicyInput source) {
          return null;
        }

        @Override
        protected PolicyInput performRemove(PolicyInput target, PolicyInput removePolicy) {
          return null;
        }

        @Override
        protected boolean performIsValid(PolicyInput policyInput) {
          return true;
        }
      };

  @Test
  void testCombineValidation() {
    assertThrows(InternalTpsErrorException.class, () -> testPolicy.combine(testPolicyInput, null));
    assertThrows(InternalTpsErrorException.class, () -> testPolicy.combine(null, testPolicyInput));
  }

  @Test
  void testRemoveValidation() {
    assertThrows(InternalTpsErrorException.class, () -> testPolicy.remove(testPolicyInput, null));
    assertThrows(InternalTpsErrorException.class, () -> testPolicy.remove(null, testPolicyInput));
  }

  @Test
  void testIsValidValidation() {
    assertThrows(InternalTpsErrorException.class, () -> testPolicy.isValid(testPolicyInput));
  }
}
