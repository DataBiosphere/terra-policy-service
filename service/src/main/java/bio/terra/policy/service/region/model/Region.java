package bio.terra.policy.service.region.model;

public class Region {
  private String name;
  private String description;
  private Region[] regions;
  private String[] datacenters;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Region[] getRegions() {
    return regions;
  }

  public void setRegions(Region[] regions) {
    this.regions = regions;
  }

  public String[] getDatacenters() {
    return datacenters;
  }

  public void setDatacenters(String[] datacenters) {
    this.datacenters = datacenters;
  }
}
