package bio.terra.policy.app.controller;

import bio.terra.common.exception.ConflictException;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.generated.api.TpsApi;
import bio.terra.policy.generated.model.ApiTpsDatacenterList;
import bio.terra.policy.generated.model.ApiTpsPaoCreateRequest;
import bio.terra.policy.generated.model.ApiTpsPaoGetResult;
import bio.terra.policy.generated.model.ApiTpsPaoReplaceRequest;
import bio.terra.policy.generated.model.ApiTpsPaoSourceRequest;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateRequest;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateResult;
import bio.terra.policy.generated.model.ApiTpsPolicyInputs;
import bio.terra.policy.service.pao.PaoService;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.policy.model.PolicyUpdateResult;
import bio.terra.policy.service.region.RegionService;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** Main TPS controller */
@Controller
public class TpsApiController implements TpsApi {
  private final PaoService paoService;
  private final RegionService regionService;

  @Autowired
  public TpsApiController(PaoService paoService, RegionService regionService) {
    this.paoService = paoService;
    this.regionService = regionService;
  }

  // -- Policy Queries --
  // TODO: PF-1733 Next step is to add group membership constraint

  // -- Policy Attribute Objects --
  @Override
  public ResponseEntity<Void> createPao(ApiTpsPaoCreateRequest body) {
    paoService.createPao(
        body.getObjectId(),
        ConversionUtils.componentFromApi(body.getComponent()),
        ConversionUtils.objectTypeFromApi(body.getObjectType()),
        ConversionUtils.policyInputsFromApi(body.getAttributes()));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deletePao(UUID objectId) {
    paoService.deletePao(objectId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> validateDatacenterAllowed(
      UUID objectId, String datacenter, String platform) {
    Pao pao = paoService.getPao(objectId);
    if (!regionService.isDatacenterAllowedByPao(pao, datacenter, platform)) {
      throw new ConflictException(
          String.format(
              "Data center '%s' is not allowed per the effective region constraint.", datacenter),
          regionService
              .getPolicyInputDataCenterCodes(pao.getEffectiveAttributes(), platform)
              .stream()
              .toList());
    }

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiTpsDatacenterList> listValidDatacenters(UUID objectId, String platform) {
    Pao pao = paoService.getPao(objectId);
    HashSet<String> datacenters =
        regionService.getPolicyInputDataCenterCodes(pao.getEffectiveAttributes(), platform);
    ApiTpsDatacenterList response = new ApiTpsDatacenterList();
    response.addAll(datacenters);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsDatacenterList> listValidByPolicyInput(
      String platform, ApiTpsPolicyInputs policyInputs) {
    PolicyInputs inputs = ConversionUtils.policyInputsFromApi(policyInputs);
    HashSet<String> datacenters = regionService.getPolicyInputDataCenterCodes(inputs, platform);
    ApiTpsDatacenterList response = new ApiTpsDatacenterList();
    response.addAll(datacenters);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsPaoGetResult> getPao(UUID objectId) {
    Pao pao = paoService.getPao(objectId);
    ApiTpsPaoGetResult result = ConversionUtils.paoToApi(pao);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsPaoUpdateResult> linkPao(UUID objectId, ApiTpsPaoSourceRequest body) {
    PolicyUpdateResult result =
        paoService.linkSourcePao(
            objectId,
            body.getSourceObjectId(),
            ConversionUtils.updateModeFromApi(body.getUpdateMode()));
    ApiTpsPaoUpdateResult apiResult = ConversionUtils.updateResultToApi(result);
    return new ResponseEntity<>(apiResult, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsPaoUpdateResult> mergePao(
      UUID objectId, ApiTpsPaoSourceRequest body) {
    PolicyUpdateResult result =
        paoService.mergeFromPao(
            objectId,
            body.getSourceObjectId(),
            ConversionUtils.updateModeFromApi(body.getUpdateMode()));

    ApiTpsPaoUpdateResult apiResult = ConversionUtils.updateResultToApi(result);
    return new ResponseEntity<>(apiResult, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsPaoUpdateResult> replacePao(
      UUID objectId, ApiTpsPaoReplaceRequest body) {
    PolicyUpdateResult result =
        paoService.replacePao(
            objectId,
            ConversionUtils.policyInputsFromApi(body.getNewAttributes()),
            ConversionUtils.updateModeFromApi(body.getUpdateMode()));

    ApiTpsPaoUpdateResult apiResult = ConversionUtils.updateResultToApi(result);
    return new ResponseEntity<>(apiResult, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsPaoUpdateResult> updatePao(
      UUID objectId, ApiTpsPaoUpdateRequest body) {
    PolicyUpdateResult result =
        paoService.updatePao(
            objectId,
            ConversionUtils.policyInputsFromApi(body.getAddAttributes()),
            ConversionUtils.policyInputsFromApi(body.getRemoveAttributes()),
            ConversionUtils.updateModeFromApi(body.getUpdateMode()));
    ApiTpsPaoUpdateResult apiResult = ConversionUtils.updateResultToApi(result);
    return new ResponseEntity<>(apiResult, HttpStatus.OK);
  }
}
