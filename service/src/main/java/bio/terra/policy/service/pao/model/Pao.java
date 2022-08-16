package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.model.PolicyInputs;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Pao {
  private final UUID objectId;
  private final PaoComponent component;
  private final PaoObjectType objectType;
  private PolicyInputs attributes;
  private PolicyInputs effectiveAttributes;
  private Set<UUID> sourceObjectIds;
  private UUID predecessorId;

  public Pao(
      UUID objectId,
      PaoComponent component,
      PaoObjectType objectType,
      PolicyInputs attributes,
      PolicyInputs effectiveAttributes,
      Set<UUID> sourceObjectIds,
      UUID predecessorId) {
    this.objectId = objectId;
    this.component = component;
    this.objectType = objectType;
    this.attributes = attributes;
    this.effectiveAttributes = effectiveAttributes;
    this.sourceObjectIds = sourceObjectIds;
    this.predecessorId = predecessorId;
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

  public void setSourceObjectIds(Set<UUID> sourceObjectIds) {
    this.sourceObjectIds = sourceObjectIds;
  }

  public UUID getPredecessorId() {
    return predecessorId;
  }

  public void setPredecessorId(UUID predecessorId) {
    this.predecessorId = predecessorId;
  }

  public Pao duplicateWithoutConflicts() {
    return new Builder()
        .setObjectId(objectId)
        .setComponent(component)
        .setObjectType(objectType)
        .setAttributes(attributes.duplicateWithoutConflicts()) // there are never conflicts in here
        .setEffectiveAttributes(effectiveAttributes.duplicateWithoutConflicts())
        .setSourceObjectIds(new HashSet<>(sourceObjectIds))
        .setPredecessorId(predecessorId)
        .build();
  }

  public static class Builder {
    private UUID objectId;
    private PaoComponent component;
    private PaoObjectType objectType;
    private PolicyInputs attributes;
    private PolicyInputs effectiveAttributes;
    private Set<UUID> sourceObjectIds;
    private UUID predecessorId;

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

    public Builder setEffectiveAttributes(PolicyInputs effectiveAttributes) {
      this.effectiveAttributes = effectiveAttributes;
      return this;
    }

    public Builder setSourceObjectIds(Set<UUID> sourceObjectIds) {
      this.sourceObjectIds = sourceObjectIds;
      return this;
    }

    public Builder setPredecessorId(UUID predecessorId) {
      this.predecessorId = predecessorId;
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
          predecessorId);
    }
  }
}
