package bio.terra.policy.service.region.model;

/** Location contains the geographic name and the region (a.k.a data center ids). */
public class Location {
  // Geographic name of the location (e.g. asia, usa, iowa)
  private String name;
  private String description;
  private Location[] locations;
  private String[] regions;

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

  public Location[] getLocations() {
    return locations;
  }

  public void setLocations(Location[] locations) {
    this.locations = locations;
  }

  public String[] getRegions() {
    return regions;
  }

  public void setRegions(String[] regions) {
    this.regions = regions;
  }
}