package bio.terra.policy.app.controller;

import bio.terra.common.exception.ConflictException;
import bio.terra.policy.common.MetricsUtils;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.generated.api.TpsApi;
import bio.terra.policy.generated.model.ApiTpsLocation;
import bio.terra.policy.generated.model.ApiTpsPaoCreateRequest;
import bio.terra.policy.generated.model.ApiTpsPaoExplainResult;
import bio.terra.policy.generated.model.ApiTpsPaoGetResult;
import bio.terra.policy.generated.model.ApiTpsPaoReplaceRequest;
import bio.terra.policy.generated.model.ApiTpsPaoSourceRequest;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateRequest;
import bio.terra.policy.generated.model.ApiTpsPaoUpdateResult;
import bio.terra.policy.generated.model.ApiTpsPolicyExplainSource;
import bio.terra.policy.generated.model.ApiTpsPolicyExplanation;
import bio.terra.policy.generated.model.ApiTpsPolicyInputs;
import bio.terra.policy.generated.model.ApiTpsRegions;
import bio.terra.policy.service.pao.PaoService;
import bio.terra.policy.service.pao.graph.model.ExplainGraph;
import bio.terra.policy.service.pao.graph.model.ExplainGraphNode;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import bio.terra.policy.service.pao.model.PaoUpdateMode;
import bio.terra.policy.service.policy.model.PolicyUpdateResult;
import bio.terra.policy.service.region.RegionService;
import bio.terra.policy.service.region.model.Location;
import java.util.List;
import java.util.Set;
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

  // -- Policy Attribute Objects --
  @Override
  public ResponseEntity<Void> createPao(ApiTpsPaoCreateRequest body) {
    paoService.createPao(
        body.getObjectId(),
        PaoComponent.fromApi(body.getComponent()),
        PaoObjectType.fromApi(body.getObjectType()),
        ConversionUtils.policyInputsFromApi(body.getAttributes()));
    MetricsUtils.incrementPaoCreation();
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deletePao(UUID objectId) {
    paoService.deletePao(objectId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiTpsPaoExplainResult> explainPao(UUID objectId, Integer depth) {
    // Build the explain graph
    ExplainGraph graph = paoService.explainPao(objectId, depth);

    // Convert the sources to API form
    List<ApiTpsPolicyExplainSource> explainSources =
        graph.explainPaos().stream()
            .map(
                ep ->
                    new ApiTpsPolicyExplainSource()
                        .component(ep.getComponent().toApi())
                        .objectType(ep.getObjectType().toApi())
                        .objectId(ep.getObjectId())
                        .deleted(ep.getDeleted())
                        .createdDate(ep.getCreated().toString())
                        .lastUpdatedDate(ep.getLastUpdated().toString()))
            .toList();

    // Convert the explanations to API form
    List<ApiTpsPolicyExplanation> explanations =
        graph.explainGraph().stream().map(this::convertExplanation).toList();

    var result =
        new ApiTpsPaoExplainResult()
            .depth(depth)
            .objectId(objectId)
            .explainObjects(explainSources)
            .explanation(explanations);

    MetricsUtils.incrementPaoExplain();

    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  private ApiTpsPolicyExplanation convertExplanation(ExplainGraphNode node) {
    return new ApiTpsPolicyExplanation()
        .objectId(node.getObjectId())
        .policyInput(ConversionUtils.policyInputToApi(node.getPolicyInput()))
        .policyExplanations(node.getSources().stream().map(this::convertExplanation).toList());
  }

  @Override
  public ResponseEntity<ApiTpsPaoGetResult> getPao(UUID objectId, Boolean includeDeleted) {
    if (includeDeleted == null) {
      includeDeleted = false;
    }
    Pao pao = paoService.getPao(objectId, includeDeleted);
    ApiTpsPaoGetResult result = ConversionUtils.paoToApi(pao);
    MetricsUtils.incrementPaoGet();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsRegions> getRegions(String platform, String location) {
    ApiTpsRegions result = new ApiTpsRegions();

    var locations = regionService.getLocationsForPlatform(location, platform);

    if (locations == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    result.addAll(locations.stream().map(Location::getCloudRegion).toList());
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsLocation> getLocationInfo(String platform, String locationName) {
    Location location = regionService.getOntology(locationName, platform);
    if (location == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    ApiTpsLocation result = ConversionUtils.regionToApi(location);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsPaoUpdateResult> linkPao(UUID objectId, ApiTpsPaoSourceRequest body) {
    PolicyUpdateResult result =
        paoService.linkSourcePao(
            objectId, body.getSourceObjectId(), PaoUpdateMode.fromApi(body.getUpdateMode()));
    ApiTpsPaoUpdateResult apiResult = ConversionUtils.updateResultToApi(result);
    return new ResponseEntity<>(apiResult, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<ApiTpsPaoGetResult>> listPaos(List<UUID> objectIds) {
    return new ResponseEntity<>(
        paoService.listPaos(objectIds).stream().map(ConversionUtils::paoToApi).toList(),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsRegions> listValidByPolicyInput(
      String platform, ApiTpsPolicyInputs policyInputs) {
    PolicyInputs inputs = ConversionUtils.policyInputsFromApi(policyInputs);
    Set<Location> locations = regionService.getPolicyInputLocationsForPlatform(inputs, platform);
    ApiTpsRegions response = new ApiTpsRegions();
    response.addAll(locations.stream().map(Location::getCloudRegion).toList());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsRegions> listValidRegions(UUID objectId, String platform) {
    Pao pao = paoService.getPao(objectId);
    var locations =
        regionService.getPolicyInputLocationsForPlatform(pao.getEffectiveAttributes(), platform);
    ApiTpsRegions response = new ApiTpsRegions();
    response.addAll(locations.stream().map(Location::getCloudRegion).toList());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiTpsPaoUpdateResult> mergePao(
      UUID objectId, ApiTpsPaoSourceRequest body) {
    PolicyUpdateResult result =
        paoService.mergeFromPao(
            objectId, body.getSourceObjectId(), PaoUpdateMode.fromApi(body.getUpdateMode()));

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
            PaoUpdateMode.fromApi(body.getUpdateMode()));

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
            PaoUpdateMode.fromApi(body.getUpdateMode()));
    ApiTpsPaoUpdateResult apiResult = ConversionUtils.updateResultToApi(result);
    return new ResponseEntity<>(apiResult, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> validateRegionAllowed(UUID objectId, String region, String platform) {
    Pao pao = paoService.getPao(objectId);
    if (!regionService.isCloudRegionAllowedByPao(pao, region, platform)) {
      throw new ConflictException(
          String.format("Region '%s' is not allowed per the effective region constraint.", region),
          regionService
              .getPolicyInputLocationsForPlatform(pao.getEffectiveAttributes(), platform)
              .stream()
              .map(Location::getCloudRegion)
              .toList());
    }

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
