package bio.terra.policy.db;

import bio.terra.policy.common.exception.PolicyObjectNotFoundException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.db.exception.DuplicateObjectException;
import bio.terra.policy.library.configuration.TpsDatabaseConfiguration;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/*
 * We cannot use the @WriteTransaction / @ReadTransaction short cuts because we need to
 * specify the transaction manager.
 */
@Component
public class PaoDao {
  private final Logger logger = LoggerFactory.getLogger(PaoDao.class);
  private final TpsDatabaseConfiguration tpsDatabaseConfiguration;
  private final NamedParameterJdbcTemplate tpsJdbcTemplate;

  @Autowired
  public PaoDao(TpsDatabaseConfiguration tpsDatabaseConfiguration) {
    this.tpsDatabaseConfiguration = tpsDatabaseConfiguration;
    this.tpsJdbcTemplate = new NamedParameterJdbcTemplate(tpsDatabaseConfiguration.getDataSource());
  }

  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
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
      tpsJdbcTemplate.update(sql, params);
      logger.info("Inserted record for pao {}", objectId);
    } catch (DuplicateKeyException e) {
      throw new DuplicateObjectException(
          "Duplicate policy attributes object with objectId " + objectId);
    }

    // Second we store the policy inputs
    final String setsql =
        "INSERT INTO attribute_set(set_id, namespace, name, properties)"
            + " VALUES(:set_id, :namespace, :name, cast(:properties AS jsonb))";

    for (PolicyInput input : inputs.getInputs().values()) {
      MapSqlParameterSource setparams =
          new MapSqlParameterSource()
              .addValue("set_id", setId)
              .addValue("namespace", input.getNamespace())
              .addValue("name", input.getName())
              .addValue("properties", DbSerDes.propertiesToJson(input.getAdditionalData()));

      tpsJdbcTemplate.update(setsql, setparams);
      logger.info(
          "Inserted record for pao set id {}, namespace {}, name {}",
          setId,
          input.getNamespace(),
          input.getName());
    }
  }

  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
  public void deletePao(UUID objectId) {
    try {
      // Lookup the policy object
      DbPao dbPao = getDbPao(objectId);

      // Delete associated attribute set(s)
      deleteAttributeSet(dbPao.attributeSetId());
      if (!dbPao.attributeSetId().equals(dbPao.effectiveSetId())) {
        deleteAttributeSet(dbPao.effectiveSetId());
      }

      // Delete the policy object
      final String sql = "DELETE FROM policy_object WHERE object_id = :object_id";
      MapSqlParameterSource params =
          new MapSqlParameterSource().addValue("object_id", objectId.toString());
      tpsJdbcTemplate.update(sql, params);
    } catch (EmptyResultDataAccessException e) {
      // Delete throws no error on not found
    }
  }

  private void deleteAttributeSet(String setId) {
    final String sql = "DELETE FROM attribute_set WHERE set_id = :set_id";
    final var params = new MapSqlParameterSource().addValue("set_id", setId);
    tpsJdbcTemplate.update(sql, params);
  }

  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      readOnly = true,
      transactionManager = "tpsTransactionManager")
  public Pao getPao(UUID objectId) {
    try {
      DbPao dbPao = getDbPao(objectId);
      PolicyInputs attributeSet = getAttributeSet(dbPao.attributeSetId());
      PolicyInputs effectiveSet;
      if (dbPao.attributeSetId().equals(dbPao.effectiveSetId())) {
        effectiveSet = attributeSet;
      } else {
        effectiveSet = getAttributeSet(dbPao.effectiveSetId());
      }
      return new Pao.Builder()
          .setObjectId(dbPao.objectId())
          .setComponent(dbPao.component())
          .setObjectType(dbPao.objectType())
          .setAttributes(attributeSet)
          .setEffectiveAttributes(effectiveSet)
          .setInConflict(dbPao.inConflict())
          .build();
    } catch (EmptyResultDataAccessException e) {
      throw new PolicyObjectNotFoundException("Policy object not found: " + objectId);
    }
  }

  private DbPao getDbPao(UUID objectId) {
    final String sql =
        "SELECT object_id, component, object_type, in_conflict, attribute_set_id, effective_set_id"
            + " FROM policy_object WHERE object_id = :object_id";

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("object_id", objectId.toString());

    return tpsJdbcTemplate.queryForObject(
        sql,
        params,
        (rs, rowNum) -> {
          return new DbPao(
              UUID.fromString(rs.getString("object_id")),
              PaoComponent.fromDb(rs.getString("component")),
              PaoObjectType.fromDb(rs.getString("object_type")),
              rs.getBoolean("in_conflict"),
              rs.getString("attribute_set_id"),
              rs.getString("effective_set_id"));
        });
  }

  private PolicyInputs getAttributeSet(String setId) {
    final String sql =
        "SELECT namespace, name, properties FROM attribute_set WHERE set_id = :set_id";

    final var params = new MapSqlParameterSource().addValue("set_id", setId);

    List<PolicyInput> inputList =
        tpsJdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> {
              return new PolicyInput(
                  rs.getString("namespace"),
                  rs.getString("name"),
                  DbSerDes.jsonToProperties(rs.getString("properties")));
            });

    return PolicyInputs.fromDb(inputList);
  }
}
