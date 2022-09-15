package bio.terra.policy.db;

import bio.terra.policy.common.exception.PolicyObjectNotFoundException;
import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.common.model.PolicyName;
import bio.terra.policy.db.exception.DuplicateObjectException;
import bio.terra.policy.library.configuration.TpsDatabaseConfiguration;
import bio.terra.policy.service.pao.graph.model.GraphNode;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.pao.model.PaoComponent;
import bio.terra.policy.service.pao.model.PaoObjectType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
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
  private static final RowMapper<DbPao> DB_PAO_ROW_MAPPER =
      (rs, rowNum) -> {
        String[] sourcesArray = (String[]) rs.getArray("sources").getArray();
        Set<String> sources = new HashSet<>(Arrays.asList(sourcesArray));
        return new DbPao(
            UUID.fromString(rs.getString("object_id")),
            PaoComponent.fromDb(rs.getString("component")),
            PaoObjectType.fromDb(rs.getString("object_type")),
            sources,
            rs.getString("attribute_set_id"),
            rs.getString("effective_set_id"),
            rs.getBoolean("deleted"));
      };

  private static final RowMapper<DbAttribute> DB_ATTRIBUTE_SET_ROW_MAPPER =
      (rs, rowNum) -> {
        String[] conflictsArray = (String[]) rs.getArray("conflicts").getArray();
        Set<UUID> conflicts =
            Arrays.stream(conflictsArray).map(UUID::fromString).collect(Collectors.toSet());

        return new DbAttribute(
            rs.getString("set_id"),
            new PolicyInput(
                new PolicyName(rs.getString("namespace"), rs.getString("name")),
                DbAdditionalData.fromDb(rs.getString("properties")),
                conflicts));
      };

  private final Logger logger = LoggerFactory.getLogger(PaoDao.class);
  private final NamedParameterJdbcTemplate tpsJdbcTemplate;

  @Autowired
  public PaoDao(TpsDatabaseConfiguration tpsDatabaseConfiguration) {
    this.tpsJdbcTemplate = new NamedParameterJdbcTemplate(tpsDatabaseConfiguration.getDataSource());
  }

  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
  public void createPao(
      UUID objectId, PaoComponent component, PaoObjectType objectType, PolicyInputs inputs) {

    // Store the attribute set twice: once as the object's set and once as its effective set.
    // We could optimize this case, but the logic is cleaner if we treat them distinctly from the
    // outset.
    String attributeSetId = UUID.randomUUID().toString();
    String effectiveSetId = UUID.randomUUID().toString();
    createAttributeSet(attributeSetId, inputs);
    createAttributeSet(effectiveSetId, inputs);

    createDbPao(
        objectId,
        component.getDbComponent(),
        objectType.getDbObjectType(),
        "",
        attributeSetId,
        effectiveSetId);
  }

  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tpsTransactionManager")
  public void deletePao(UUID objectId) {
    final Set<UUID> dependents = getDependentIds(objectId);
    setPaoDeleted(objectId, true);

    if (dependents.isEmpty()) {
      DbPao dbPao = getDbPao(objectId);

      // First Pass: Build a map of all PAOs in our subgraph and note if they are flagged for
      // deletion.
      HashMap<UUID, Boolean> removablePaoMap = new HashMap<>();
      HashMap<UUID, DbPao> subgraphMap = new HashMap<>();
      walkDeleteSubgraph(subgraphMap, removablePaoMap, dbPao);

      // Second Pass: iterate through our graph, for each PAO:
      // Recursively check its dependents and verify it's still removable.
      // If all dependents are flagged for deletion and exist in our known subgraph,
      // then the PAO is still removable.
      HashMap<UUID, Set<DbPao>> dependentMap = new HashMap<>();
      walkDeleteDependents(subgraphMap, removablePaoMap, dependentMap, dbPao);

      // Finally - remove PAOs that are still removable
      removablePaoMap.forEach(
          (UUID id, Boolean toRemove) -> {
            if (toRemove) {
              removeDbPao(getDbPao(id));
            }
          });
    }
  }

  @Retryable(interceptor = "transactionRetryInterceptor")
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      readOnly = true,
      transactionManager = "tpsTransactionManager")
  public Pao getPao(UUID objectId) {
    DbPao dbPao = getDbPao(objectId);
    Map<String, PolicyInputs> attributeSetMap =
        getAttributeSets(List.of(dbPao.attributeSetId(), dbPao.effectiveSetId()));
    return Pao.fromDb(dbPao, attributeSetMap);
  }

  // -- Graph Walk Methods --
  // These methods are intentionally without transaction annotations. They are used by the policy
  // update
  // process. That process may do multiple reads of the database followed by a big update. The
  // transaction
  // is managed outside of the DAO.

  /**
   * Given a list of PAO ids, return PAOs
   *
   * @param objectIdList UUIDs of Policy Attribute Objects
   * @return List of Pao objects
   */
  public List<Pao> getPaos(List<UUID> objectIdList) {
    List<Pao> paoList = new ArrayList<>();
    if (objectIdList.isEmpty()) {
      // Nothing to do
      return paoList;
    }

    List<DbPao> dbPaoList = getDbPaos(objectIdList);
    List<String> setIdList = new ArrayList<>();
    for (DbPao dbPao : dbPaoList) {
      setIdList.add(dbPao.attributeSetId());
      setIdList.add(dbPao.effectiveSetId());
    }

    // Gather all of the attribute sets
    Map<String, PolicyInputs> attributeSetMap = getAttributeSets(setIdList);

    for (DbPao dbPao : dbPaoList) {
      paoList.add(Pao.fromDb(dbPao, attributeSetMap));
    }

    return paoList;
  }

  /**
   * Recursively build out a map of which PAOs are in the subgraph and note which PAOs have been
   * flagged for deletion.
   *
   * @param subgraphMap a map of PAO id to dbPao that will be filled in during the recursive calls
   * @param subgraphStatusMap map of PAO id to 'deleted' flag status that will be filled in during
   *     recursive calls.
   * @param pao the PAO currently being evaluated. On first call, this would be the root of the
   *     subgraph.
   */
  private void walkDeleteSubgraph(
      HashMap<UUID, DbPao> subgraphMap, HashMap<UUID, Boolean> subgraphStatusMap, DbPao pao) {
    subgraphStatusMap.put(pao.objectId(), pao.deleted());
    subgraphMap.put(pao.objectId(), pao);

    for (var source : pao.sources()) {
      walkDeleteSubgraph(subgraphMap, subgraphStatusMap, getDbPao(UUID.fromString(source)));
    }
  }

  /**
   * Recursive call to check all dependents of a given PAO and update the PAOs removability.
   *
   * @param subgraphMap a map of PAO id to dbPao. This map gets checked for subgraph membership as
   *     we recurse through dependents.
   * @param subgraphStatusMap map of PAO id to 'deleted' flag status
   * @param dependentMap a map of PAO id to all of that PAOs dependents. This serves as a cache and
   *     is filled in during recursive calls.
   * @param pao the PAO currently being evaluated. On first call, this would be the root of the
   *     subgraph.
   */
  private void walkDeleteDependents(
      HashMap<UUID, DbPao> subgraphMap,
      HashMap<UUID, Boolean> subgraphStatusMap,
      HashMap<UUID, Set<DbPao>> dependentMap,
      DbPao pao) {
    Set<DbPao> dependents = new HashSet<>();

    if (pao == null) return;

    if (dependentMap.containsKey(pao.objectId())) {
      // we have a cached answer for this PAO
      dependents = dependentMap.get(pao.objectId());
    } else {
      // use a BFS to build a dependency list
      Queue<UUID> queue = new LinkedList<>();
      queue.addAll(getDependentIds(pao.objectId()));

      while (!queue.isEmpty()) {
        var depId = queue.poll();
        var depPao = getDbPao(depId);
        dependents.add(depPao);
        queue.addAll(getDependentIds(depId));
      }

      dependentMap.put(pao.objectId(), dependents);
    }

    for (var dependent : dependents) {
      if (!subgraphMap.containsKey(dependent.objectId()) || !dependent.deleted()) {
        // if any dependent is not part of the subgraph or is not flagged for removal
        // then the current PAO cannot be removed.
        subgraphStatusMap.put(pao.objectId(), false);
        break;
      }
    }

    for (var source : pao.sources()) {
      // recursive step to continue checking all the current PAOs sources
      // use the subgraphMap so lookup PAOs so that we don't keep querying the db
      walkDeleteDependents(
          subgraphMap, subgraphStatusMap, dependentMap, subgraphMap.get(UUID.fromString(source)));
    }
  }

  /**
   * Given a source id, find all of the dependents and return their ids
   *
   * @param sourceId source to hunt for
   * @return Set of dependent UUIDs that reference that source
   */
  public Set<UUID> getDependentIds(UUID sourceId) {
    final String sql = "SELECT object_id FROM policy_object WHERE :source_id = ANY(sources)";

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("source_id", sourceId.toString());

    return new HashSet<>(
        tpsJdbcTemplate.query(
            sql, params, (rs, rowNum) -> UUID.fromString(rs.getString("object_id"))));
  }

  /**
   * Update all of the changed Paos by processing the graph nodes. We use the graph nodes because
   * they hold the initial version of the Pao and the computed version of the Pao, so we can update
   * only what changed.
   *
   * @param changeList list of modified paos in graph nodes
   */
  public void updatePaos(List<GraphNode> changeList) {
    changeList.forEach(this::updatePao);
  }

  /**
   * Update one Pao if attribute set changed: rewrite attribute set if effective set changed:
   * rewrite effective set if sources list changed, update the policy object with new sources array
   *
   * @param change graph node that has the initial and newly computed Paos
   */
  private void updatePao(GraphNode change) {
    // The graph node holds the changes we need to make to the PAO sources and attribute sets
    Pao pao = change.getPao();
    PolicyInputs attributes = change.getPolicyAttributes();
    PolicyInputs effectiveAttributes = change.getEffectivePolicyAttributes();

    // Get the dbPao and the attribute sets from the db for comparison
    DbPao dbPao = getDbPao(pao.getObjectId());
    Map<String, PolicyInputs> attributeSetMap =
        getAttributeSets(List.of(dbPao.attributeSetId(), dbPao.effectiveSetId()));
    PolicyInputs dbAttributes = attributeSetMap.get(dbPao.attributeSetId());
    PolicyInputs dbEffectiveAttributes = attributeSetMap.get(dbPao.effectiveSetId());

    // Update attributes if changed
    if (!attributes.equals(dbAttributes)) {
      String attributeSetId = dbPao.attributeSetId();
      deleteAttributeSet(attributeSetId);
      createAttributeSet(attributeSetId, attributes);
    }

    // Update effective attributes if changed
    if (!effectiveAttributes.equals(dbEffectiveAttributes)) {
      String effectiveSetId = dbPao.effectiveSetId();
      deleteAttributeSet(effectiveSetId);
      createAttributeSet(effectiveSetId, effectiveAttributes);
    }

    // Update sources if changed
    Set<UUID> dbSources =
        dbPao.sources().stream().map(UUID::fromString).collect(Collectors.toSet());
    if (!dbSources.equals(pao.getSourceObjectIds())) {
      final String sql =
          "UPDATE policy_object SET sources = string_to_array(:sources, ',') WHERE object_id = :object_id";

      String sourcesSqlArray = makeCsvFromUuidSet(pao.getSourceObjectIds());

      MapSqlParameterSource params =
          new MapSqlParameterSource()
              .addValue("object_id", pao.getObjectId().toString())
              .addValue("sources", sourcesSqlArray);

      tpsJdbcTemplate.update(sql, params);
      logger.info(
          "Update sources array for pao object id {}, sources {}",
          pao.getObjectId().toString(),
          sourcesSqlArray);
    }
  }

  private void removeDbPao(DbPao dbPao) {
    try {
      // Delete associated attribute set(s)
      deleteAttributeSet(dbPao.attributeSetId());
      deleteAttributeSet(dbPao.effectiveSetId());

      // Delete the policy object
      final String sql = "DELETE FROM policy_object WHERE object_id=:object_id";
      MapSqlParameterSource params =
          new MapSqlParameterSource().addValue("object_id", dbPao.objectId().toString());
      tpsJdbcTemplate.update(sql, params);
    } catch (PolicyObjectNotFoundException e) {
      // Delete throws no error on not found
    }
  }

  private void setPaoDeleted(UUID objectId, boolean isDeleted) {
    final String sql = "UPDATE policy_object SET deleted=:deleted WHERE object_id=:object_id";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("object_id", objectId.toString())
            .addValue("deleted", isDeleted);
    tpsJdbcTemplate.update(sql, params);
  }

  /**
   * The JdbcTemplate doesn't have a nice way to pass arrays into and out of Postgres. Since we use
   * UUIDs, we know there are no commas, so we can build the CSV and use the Postgres
   * string_to_array builtin to set the array value.
   *
   * @param uuidSet input set of UUIDs
   * @return csv of strings
   */
  private String makeCsvFromUuidSet(Set<UUID> uuidSet) {
    return String.join(",", uuidSet.stream().map(UUID::toString).toList());
  }

  private void createAttributeSet(String setId, PolicyInputs inputs) {
    final String setsql =
        """
        INSERT INTO attribute_set(set_id, namespace, name, properties, conflicts)
        VALUES(:set_id, :namespace, :name, cast(:properties AS jsonb), string_to_array(:conflicts,','))
        """;

    for (PolicyInput input : inputs.getInputs().values()) {
      String conflictCsv = makeCsvFromUuidSet(input.getConflicts());
      MapSqlParameterSource setparams =
          new MapSqlParameterSource()
              .addValue("set_id", setId)
              .addValue("namespace", input.getPolicyName().getNamespace())
              .addValue("name", input.getPolicyName().getName())
              .addValue("properties", DbAdditionalData.toDb(input.getAdditionalData()))
              .addValue("conflicts", conflictCsv);
      tpsJdbcTemplate.update(setsql, setparams);
      logger.info(
          "Inserted record for pao set id {}, policy {}, conflicts {}",
          setId,
          input.getPolicyName(),
          conflictCsv);
    }
  }

  private void createDbPao(
      UUID objectId,
      String component,
      String objectType,
      String sources,
      String attributeSetId,
      String effectiveSetId) {
    final String sql =
        """
        INSERT INTO policy_object
          (object_id, component, object_type, sources, attribute_set_id, effective_set_id)
        VALUES
          (:object_id, :component, :object_type, string_to_array(:sources, ','), :attribute_set_id, :effective_set_id)
        """;

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("object_id", objectId)
            .addValue("component", component)
            .addValue("object_type", objectType)
            .addValue("sources", sources)
            .addValue("attribute_set_id", attributeSetId)
            .addValue("effective_set_id", effectiveSetId);

    try {
      tpsJdbcTemplate.update(sql, params);
      logger.info("Inserted record for pao {}", objectId);
    } catch (DuplicateKeyException e) {
      throw new DuplicateObjectException(
          "Duplicate policy attributes object with objectId " + objectId);
    }
  }

  private void deleteAttributeSet(String setId) {
    final String sql = "DELETE FROM attribute_set WHERE set_id = :set_id";
    final var params = new MapSqlParameterSource().addValue("set_id", setId);
    tpsJdbcTemplate.update(sql, params);
  }

  private DbPao getDbPao(UUID objectId) {
    final String sql =
        """
        SELECT object_id, component, object_type, attribute_set_id, effective_set_id, sources, deleted
        FROM policy_object WHERE object_id = :object_id
        """;

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("object_id", objectId.toString());

    List<DbPao> dbPao = tpsJdbcTemplate.query(sql, params, DB_PAO_ROW_MAPPER);
    if (dbPao.isEmpty()) {
      throw new PolicyObjectNotFoundException("Policy object not found: " + objectId);
    }
    return dbPao.get(0);
  }

  private List<DbPao> getDbPaos(List<UUID> objectIdList) {
    final String sql =
        """
        SELECT object_id, component, object_type, attribute_set_id, effective_set_id, sources, deleted
        FROM policy_object
        WHERE object_id IN (:object_id_list)
        """;

    if (objectIdList.isEmpty()) {
      return new ArrayList<>();
    }
    List<String> stringIdList = objectIdList.stream().map(UUID::toString).toList();

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("object_id_list", stringIdList);

    return tpsJdbcTemplate.query(sql, params, DB_PAO_ROW_MAPPER);
  }

  /**
   * Build attribute sets from a list of set ids, possibly duplicate ones.
   *
   * @param setIdList list of set ids
   * @return map of set id to attribute set
   */
  private Map<String, PolicyInputs> getAttributeSets(List<String> setIdList) {
    final String sql =
        """
        SELECT set_id, namespace, name, properties, conflicts
        FROM attribute_set
        WHERE set_id IN (:set_id_list)
        """;

    var attributeSets = new HashMap<String, PolicyInputs>();
    if (setIdList.isEmpty()) {
      // Nothing to do - skip the query
      return attributeSets;
    }

    var uniqueSetIds = new HashSet<>(setIdList);
    var params = new MapSqlParameterSource().addValue("set_id_list", uniqueSetIds);

    List<DbAttribute> attributeList =
        tpsJdbcTemplate.query(sql, params, DB_ATTRIBUTE_SET_ROW_MAPPER);

    // Initialize the attributeSets with all input set ids and an empty PolicyInputs.
    // That is a valid return and covers the case where attribute sets are empty,
    // so do not have any rows in the attribute set table.
    for (String id : uniqueSetIds) {
      attributeSets.put(id, new PolicyInputs());
    }

    // For attribute rows we have, find their attribute set and add the policy
    for (DbAttribute attribute : attributeList) {
      PolicyInputs policyInputs = attributeSets.get(attribute.setId());
      policyInputs.addInput(attribute.policyInput());
    }

    return attributeSets;
  }
}
