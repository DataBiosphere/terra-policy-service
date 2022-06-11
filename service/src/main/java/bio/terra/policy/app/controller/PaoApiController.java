package bio.terra.policy.app.controller;

import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.policy.api.PaoApi;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.model.ApiPaoCreateRequest;
import bio.terra.policy.model.ApiPaoGetResult;
import bio.terra.policy.service.pao.PaoService;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PaoApiController implements PaoApi {

  private final BearerTokenFactory bearerTokenFactory;
  private final HttpServletRequest request;
  private final PaoService paoService;

  @Autowired
  public PaoApiController(
      BearerTokenFactory bearerTokenFactory, HttpServletRequest request, PaoService paoService) {
    this.bearerTokenFactory = bearerTokenFactory;
    this.request = request;
    this.paoService = paoService;
  }

  @Override
  public ResponseEntity<Void> createPao(ApiPaoCreateRequest body) {
    // TODO: permissions

    paoService.createPao(
        body.getObjectId(),
        PaoComponent.fromApi(body.getComponent()),
        PaoObjectType.fromApi(body.getObjectType()),
        PolicyInputs.fromApi(body.getAttributes()));

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deletePao(UUID objectId) {
    paoService.deletePao(objectId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiPaoGetResult> getPao(UUID objectId) {
    // TODO: permissions
    Pao pao = paoService.getPao(objectId);
    return new ResponseEntity<>(pao.toApi(), HttpStatus.OK);
  }

  private BearerToken getBearerToken() {
    return bearerTokenFactory.from(request);
  }
}
