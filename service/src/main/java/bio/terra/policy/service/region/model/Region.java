package bio.terra.policy.service.region.model;

/**
 * Region corresponds to the cloud concept of region. The id is in the form [platform].[code] where
 * code is the identifier given by the cloud platform.
 */
public class Region {
  private String id;
  private String description;
  private String code;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
