package bio.terra.policy.service.region.model;

/** Location contains the geographic name and the region (a.k.a data center ids). */
public class Location {
  private String geographicName;
  private String description;
  private Location[] locations;
  private String[] regions;

  public String getGeographicName() {
    return geographicName;
  }

  public void setGeographicName(String geographicName) {
    this.geographicName = geographicName;
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
