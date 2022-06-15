package bio.terra.policy.service.pao.model;

import bio.terra.policy.common.model.PolicyInputs;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Pao {
  private final UUID objectId;
  private final PaoComponent component;
  private final PaoObjectType objectType;
  private final PolicyInputs attributes;
  private final PolicyInputs effectiveAttributes;
  private final boolean inConflict;
  private final List<UUID> childObjectIds;

  public Pao(
      UUID objectId,
      PaoComponent component,
      PaoObjectType objectType,
      PolicyInputs attributes,
      PolicyInputs effectiveAttributes,
      boolean inConflict,
      List<UUID> childObjectIds) {
    this.objectId = objectId;
    this.component = component;
    this.objectType = objectType;
    this.attributes = attributes;
    this.effectiveAttributes = effectiveAttributes;
    this.inConflict = inConflict;
    this.childObjectIds = childObjectIds;
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

  public PolicyInputs getEffectiveAttributes() {
    return effectiveAttributes;
  }

  public boolean isInConflict() {
    return inConflict;
  }

  public List<UUID> getChildObjectIds() {
    return childObjectIds;
  }

  public static class Builder {
    private UUID objectId;
    private PaoComponent component;
    private PaoObjectType objectType;
    private PolicyInputs attributes;
    private PolicyInputs effectiveAttributes;
    private boolean inConflict;
    private List<UUID> childObjectIds;

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

    public Builder setInConflict(boolean inConflict) {
      this.inConflict = inConflict;
      return this;
    }

    public Builder setChildObjectIds(List<UUID> childObjectIds) {
      this.childObjectIds = childObjectIds;
      return this;
    }

    public Pao build() {
      if (childObjectIds == null) {
        childObjectIds = new ArrayList<>();
      }
      return new Pao(
          objectId,
          component,
          objectType,
          attributes,
          effectiveAttributes,
          inConflict,
          childObjectIds);
    }
  }
}
