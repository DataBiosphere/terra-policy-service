package bio.terra.policy.service.pao.graph.model;

import bio.terra.policy.db.DbPao;

public class DeleteGraphNode {
  private final DbPao pao;
  private Boolean isRemovable;

  public DeleteGraphNode(DbPao pao, Boolean isRemovable) {
    this.pao = pao;
    this.isRemovable = isRemovable;
  }

  public DbPao getPao() {
    return pao;
  }

  public Boolean getRemovable() {
    return isRemovable;
  }

  public void setRemovable(Boolean removable) {
    isRemovable = removable;
  }
}
