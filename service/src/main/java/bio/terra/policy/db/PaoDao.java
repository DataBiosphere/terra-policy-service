package bio.terra.policy.db;

import bio.terra.common.db.WriteTransaction;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.db.exception.DuplicateObjectException;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaoDao {
  private final Logger logger = LoggerFactory.getLogger(PaoDao.class);
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public PaoDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @WriteTransaction
  public void createPao(
      UUID objectId, PaoComponent component, PaoObjectType objectType, PolicyInputs inputs) {

    final String sql =
        "INSERT INTO policy_object (object_id, component, object_type, children, in_conflict,"
            + " attribute_set_id, effective_set_id)"
            + " VALUES (:object_id, :component, :object_type, '{}', false,"
            + " :attribute_set_id, :effective_set_id)";

    final String setId = UUID.randomUUID().toString();

    // First we store the policy object with a "forward reference" to the set.
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("object_id", objectId.toString())
            .addValue("component", component.getDbComponent())
            .addValue("object_type", objectType.getDbObjectType())
            .addValue("attribute_set_id", setId)
            .addValue("effective_set_id", setId);

    try {
      jdbcTemplate.update(sql, params);
      logger.info("Inserted record for pao {}", objectId);
    } catch (DuplicateKeyException e) {
      throw new DuplicateObjectException(
          "Duplicate policy attributes object with objectId " + objectId);
    }

    // Second we store the policy inputs
    final String setsql =
        "INSERT INTO attribute_set(set_id, namespace, name, properties)"
            + " VALUES(:set_id, :namespace, :name, cast(:properties AS jsonb))";

    for (PolicyInput input : inputs.inputs().values()) {
      MapSqlParameterSource setparams =
          new MapSqlParameterSource()
              .addValue("set_id", setId)
              .addValue("namespace", input.namespace())
              .addValue("name", input.name())
              .addValue("properties", DbSerDes.propertiesToJson(input.additionalData()));

      jdbcTemplate.update(setsql, setparams);
      logger.info(
          "Inserted record for pao set id {}, namespace {}, name {}",
          setId,
          input.namespace(),
          input.name());
    }
  }
}
