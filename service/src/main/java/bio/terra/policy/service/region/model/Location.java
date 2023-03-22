package bio.terra.policy.service.region.model;

import java.util.List;

/**
 * Location is a Terra concept containing nested locations and regions corresponding to the cloud
 * concept of region.
 */
public class Location {
  // Geographic name of the location (e.g. asia, usa, iowa)
  private String name;
  private String description;
  private List<Location> locations;
  private String cloudRegion;
  private String cloudPlatform;

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

  public List<Location> getLocations() {
    return locations;
  }

  public void setLocations(List<Location> locations) {
    this.locations = locations;
  }

  public String getCloudRegion() {
    return cloudRegion;
  }

  public void setCloudRegion(String cloudRegion) {
    this.cloudRegion = cloudRegion;
  }

  public String getCloudPlatform() {
    return cloudPlatform;
  }

  public void setCloudPlatform(String cloudPlatform) {
    this.cloudPlatform = cloudPlatform;
  }
}
