package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.db.DbPao;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

public class Pao {
  private final UUID objectId;
  private final PaoComponent component;
  private final PaoObjectType objectType;
  private PolicyInputs attributes;
  private PolicyInputs effectiveAttributes;
  private Set<UUID> sourceObjectIds;
  private boolean deleted;
  private Instant created;
  private Instant lastUpdated;

  public Pao(
      UUID objectId,
      PaoComponent component,
      PaoObjectType objectType,
      PolicyInputs attributes,
      PolicyInputs effectiveAttributes,
      Set<UUID> sourceObjectIds,
      boolean deleted,
      Instant created,
      Instant lastUpdated) {
    this.objectId = objectId;
    this.component = component;
    this.objectType = objectType;
    this.attributes = attributes;
    this.effectiveAttributes = effectiveAttributes;
    this.sourceObjectIds = sourceObjectIds;
    this.deleted = deleted;
    this.created = created;
    this.lastUpdated = lastUpdated;
  }

  public UUID getObjectId() {
    return objectId;
  }

  public PaoComponent getComponent() {
    return component;
  }

  public PaoObjectType getObjectType() {
    return objectType;
  }

  public PolicyInputs getAttributes() {
    return attributes;
  }

  public void setAttributes(PolicyInputs attributes) {
    this.attributes = attributes;
  }

  public PolicyInputs getEffectiveAttributes() {
    return effectiveAttributes;
  }

  public void setEffectiveAttributes(PolicyInputs effectiveAttributes) {
    this.effectiveAttributes = effectiveAttributes;
  }

  public Set<UUID> getSourceObjectIds() {
    return sourceObjectIds;
  }

  public boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public Instant getCreated() {
    return created;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public String toShortString() {
    return String.format("%s:%s (%s)", component, objectType, objectId);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Pao.class.getSimpleName() + "[", "]")
        .add("objectId=" + objectId)
        .add("component=" + component)
        .add("objectType=" + objectType)
        .add("attributes=" + attributes)
        .add("effectiveAttributes=" + effectiveAttributes)
        .add("sourceObjectIds=" + sourceObjectIds)
        .add("deleted=" + deleted)
        .add("created=" + created)
        .add("lastUpdated=" + lastUpdated)
        .toString();
  }

  public static Pao fromDb(DbPao dbPao, Map<String, PolicyInputs> attributeSetMap) {
    return new Pao.Builder()
        .setObjectId(dbPao.objectId())
        .setComponent(dbPao.component())
        .setObjectType(dbPao.objectType())
        .setSourceObjectIds(
            dbPao.sources().stream().map(UUID::fromString).collect(Collectors.toSet()))
        .setAttributes(attributeSetMap.get(dbPao.attributeSetId()))
        .setEffectiveAttributes(attributeSetMap.get(dbPao.effectiveSetId()))
        .setDeleted(dbPao.deleted())
        .setCreated(dbPao.created())
        .setLastUpdated(dbPao.lastUpdated())
        .build();
  }

  public static class Builder {
    private UUID objectId;
    private PaoComponent component;
    private PaoObjectType objectType;
    private PolicyInputs attributes;
    private PolicyInputs effectiveAttributes;
    private Set<UUID> sourceObjectIds;
    private boolean deleted;
    private Instant created;
    private Instant lastUpdated;

    public Builder setObjectId(UUID objectId) {
      this.objectId = objectId;
      return this;
    }

    public Builder setComponent(PaoComponent component) {
      this.component = component;
      return this;
    }

    public Builder setObjectType(PaoObjectType objectType) {
      this.objectType = objectType;
      return this;
    }

    public Builder setAttributes(PolicyInputs attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder setDeleted(boolean deleted) {
      this.deleted = deleted;
      return this;
    }

    public Builder setEffectiveAttributes(PolicyInputs effectiveAttributes) {
      this.effectiveAttributes = effectiveAttributes;
      return this;
    }

    public Builder setSourceObjectIds(Set<UUID> sourceObjectIds) {
      this.sourceObjectIds = sourceObjectIds;
      return this;
    }

    public Builder setCreated(Instant created) {
      this.created = created;
      return this;
    }

    public Builder setLastUpdated(Instant lastUpdated) {
      this.lastUpdated = lastUpdated;
      return this;
    }

    public Pao build() {
      if (sourceObjectIds == null) {
        sourceObjectIds = new HashSet<>();
      }
      return new Pao(
          objectId,
          component,
          objectType,
          attributes,
          effectiveAttributes,
          sourceObjectIds,
          deleted,
          created,
          lastUpdated);
    }
  }
}
