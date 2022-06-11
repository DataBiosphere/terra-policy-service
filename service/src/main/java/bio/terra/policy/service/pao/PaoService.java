package bio.terra.policy.service.pao;

import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.db.PaoDao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaoService {
  private static final Logger logger = LoggerFactory.getLogger(PaoService.class);

  private final PaoDao paoDao;

  @Autowired
  public PaoService(PaoDao paoDao) {
    this.paoDao = paoDao;
  }

  /**
   * Create a Poliy Attribute Object in the database
   *
   * @param objectId incoming component object id
   * @param component policy-bearing component
   * @param objectType policy-bearing object
   * @param inputs policies
   */
  public void createPao(
      UUID objectId, PaoComponent component, PaoObjectType objectType, PolicyInputs inputs) {
    logger.info(
        "Create PAO id {} component {} object type {}",
        objectId,
        component.name(),
        objectType.name());

    // TODO: Validate policy inputs against the policy descriptions when those are available.

    // The DAO does the heavy lifting.
    paoDao.createPao(objectId, component, objectType, inputs);
  }
}
