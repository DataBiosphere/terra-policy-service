package bio.terra.policy.common.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.policy.common.exception.InvalidInputException;
import bio.terra.policy.model.ApiPolicyInput;
import bio.terra.policy.model.ApiPolicyInputs;
import bio.terra.policy.model.ApiPolicyPair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class PolicyInputTest {

  @Test
  public void testPolicyInput() {
    // Success cases
    var api =
        new ApiPolicyInput()
            .namespace("terra")
            .name("group-constraint")
            .addAdditionalDataItem(new ApiPolicyPair().key("group").value("ddgroup"));

    var input = PolicyInput.fromApi(api);
    assertEquals(1, input.getAdditionalData().size());

    var noDataApi = new ApiPolicyInput().namespace("terra").name("group-constraint");

    input = PolicyInput.fromApi(noDataApi);
    assertEquals(0, input.getAdditionalData().size());

    // Various null cases
    var noNamespaceApi =
        new ApiPolicyInput()
            .namespace(null)
            .name("group-constraint")
            .addAdditionalDataItem(new ApiPolicyPair().key("group").value("ddgroup"));

    assertThrows(InvalidInputException.class, () -> PolicyInput.fromApi(noNamespaceApi));

    var noNameApi =
        new ApiPolicyInput()
            .namespace(null)
            .name("group-constraint")
            .addAdditionalDataItem(new ApiPolicyPair().key("group").value("ddgroup"));

    assertThrows(InvalidInputException.class, () -> PolicyInput.fromApi(noNameApi));
  }

  @Test
  public void testPolicyInputs() {
    // Typical success case
    var api =
        new ApiPolicyInputs()
            .addInputsItem(
                new ApiPolicyInput()
                    .namespace("terra")
                    .name("group-constraint")
                    .addAdditionalDataItem(new ApiPolicyPair().key("group").value("ddgroup")))
            .addInputsItem(
                new ApiPolicyInput()
                    .namespace("terra")
                    .name("region-constraint")
                    .addAdditionalDataItem(new ApiPolicyPair().key("region").value("US")));
    var inputs = PolicyInputs.fromApi(api);
    assertEquals(2, inputs.getInputs().size());

    // Return empty map on null input
    inputs = PolicyInputs.fromApi(null);
    assertEquals(0, inputs.getInputs().size());

    // Return empty map on null list - very unlikely case, since the generated code always makes a
    // list
    var apiNullList = new ApiPolicyInputs();
    apiNullList.inputs(null);
    inputs = PolicyInputs.fromApi(apiNullList);
    assertEquals(0, inputs.getInputs().size());

    // Return empty map on empty list
    var apiEmptyList = new ApiPolicyInputs();
    inputs = PolicyInputs.fromApi(apiEmptyList);
    assertEquals(0, inputs.getInputs().size());

    // Fail on duplicate inputs

    var apiDups =
        new ApiPolicyInputs()
            .addInputsItem(
                new ApiPolicyInput()
                    .namespace("terra")
                    .name("group-constraint")
                    .addAdditionalDataItem(new ApiPolicyPair().key("group").value("ddgroup")))
            .addInputsItem(
                new ApiPolicyInput()
                    .namespace("terra")
                    .name("group-constraint")
                    .addAdditionalDataItem(new ApiPolicyPair().key("group").value("ddgroup")))
            .addInputsItem(
                new ApiPolicyInput()
                    .namespace("terra")
                    .name("region-constraint")
                    .addAdditionalDataItem(new ApiPolicyPair().key("region").value("US")));
    assertThrows(InvalidInputException.class, () -> PolicyInputs.fromApi(apiDups));
  }
}
