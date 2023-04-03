package bio.terra.policy.service.policy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.policy.common.model.Constants;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class KnownPolicyTest {
  @Test
  void testFindPolicyBaseByName() {
    verifyPolicy(
        KnownPolicy.findPolicyBaseByName(Constants.GROUP_CONSTRAINT_POLICY_NAME),
        PolicyGroupConstraint.class);

    verifyPolicy(
        KnownPolicy.findPolicyBaseByName(Constants.REGION_CONSTRAINT_POLICY_NAME),
        PolicyRegionConstraint.class);

    verifyPolicy(
        KnownPolicy.findPolicyBaseByName(Constants.PROTECTED_DATA_POLICY_NAME), PolicyLabel.class);
  }

  void verifyPolicy(Optional<PolicyBase> policy, Class<?> expected) {
    assertTrue(policy.isPresent());
    assertTrue(expected.isInstance(policy.get()));
  }
}
