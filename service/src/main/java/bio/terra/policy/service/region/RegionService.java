package bio.terra.policy.service.region;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.region.model.Location;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Component
public class RegionService {
  private static final String TERRA_REGION_CONSTRAINT = "terra:region-constraint";
  private static final String TERRA_REGION_ATTRIBUTE_NAME = "region-name";
  private static final String GLOBAL_LOCATION = "global";

  private static final Logger logger = LoggerFactory.getLogger(RegionService.class);

  // Map from location name to a list of the names of all sub-locations.
  private final Map<String, Set<Location>> subLocationsWithinLocation;
  // Object map from the location name to the location object.
  private final Map<String, Location> locationsByName;

  @Autowired
  public RegionService() {
    logger.info("Loading locations from locations.yml resource.");
    Yaml locationYaml = new Yaml(new Constructor(Location.class));
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream("static/locations.yml");
    Location rootLocation = locationYaml.load(inputStream);

    this.subLocationsWithinLocation = new HashMap<>();
    this.locationsByName = new HashMap<>();

    constructLocationMapsRecursively(rootLocation);
  }

  /** Get the list including the given location and all subLocations, filtered by platform. */
  @Nullable
  public Set<Location> getLocationsForPlatform(String locationName, String platform) {
    String queryLocation = Strings.isNullOrEmpty(locationName) ? GLOBAL_LOCATION : locationName;
    var subLocationsIncludingSelf = getSubLocationsIncludingSelf(queryLocation);
    if (subLocationsIncludingSelf == null) {
      return null;
    }
    return filterLocationsByCloudPlatform(subLocationsIncludingSelf, platform);
  }

  @Nullable
  private Set<Location> getSubLocationsIncludingSelf(String locationName) {
    Location startingLocation = locationsByName.get(locationName);
    if (startingLocation == null) {
      return null;
    }
    Set<Location> locations = new HashSet<>();
    locations.add(startingLocation);
    locations.addAll(
        Optional.ofNullable(subLocationsWithinLocation.get(locationName)).orElse(Set.of()));
    return locations;
  }

  /**
   * Gets the ontology starting from the indicated location. Rather than just returning the
   * ontology, this will filter locations by the indicated platform.
   */
  @Nullable
  public Location getOntology(String locationName, String platform) {
    String queryLocation = Strings.isNullOrEmpty(locationName) ? GLOBAL_LOCATION : locationName;
    Location location = locationsByName.get(queryLocation);

    if (location == null) {
      return null;
    }

    Location result = new Location();
    result.setName(location.getName());
    result.setDescription(location.getDescription());
    result.setCloudRegion(location.getCloudRegion());
    result.setCloudPlatform(location.getCloudPlatform());

    List<Location> subLocations = new ArrayList<>();
    if (location.getLocations() != null) {
      for (Location subLocation : location.getLocations()) {
        if (subLocation.getCloudPlatform() == null
            || subLocation.getCloudPlatform().equals(platform)) {
          subLocations.add(getOntology(subLocation.getName(), platform));
        }
      }
    }
    result.setLocations(subLocations.toArray(new Location[0]));

    return result;
  }

  @Nullable
  @VisibleForTesting
  public Location getLocation(String name) {
    return locationsByName.get(name);
  }

  public Set<Location> getPolicyInputLocationsForPlatform(PolicyInputs inputs, String platform) {
    List<String> locationNames = extractPolicyInputLocations(inputs);

    if (locationNames.isEmpty()) {
      locationNames.add(GLOBAL_LOCATION);
    }

    return locationNames.stream()
        .flatMap(
            locationName ->
                filterLocationsByCloudPlatform(getSubLocationsIncludingSelf(locationName), platform)
                    .stream())
        .collect(Collectors.toSet());
  }

  public boolean isCloudRegionAllowedByPao(Pao pao, String region, String platform) {
    List<String> locationNames = extractPolicyInputLocations(pao.getEffectiveAttributes());

    if (locationNames.isEmpty()) {
      // pao doesn't have a region constraint
      return true;
    }

    return locationNames.stream().anyMatch(n -> locationContainsCloudRegion(n, region, platform));
  }

  public boolean isSubLocation(String parentLocationName, String subLocationName) {
    Set<Location> subLocations = subLocationsWithinLocation.get(parentLocationName);
    return subLocations != null
        && subLocations.stream().anyMatch(l -> l.getName().equals(subLocationName));
  }

  public boolean locationContainsCloudRegion(
      String locationName, String regionId, String platform) {
    final Set<Location> locations = getSubLocationsIncludingSelf(locationName);
    return locations != null
        && locations.stream()
            .anyMatch(
                l -> regionId.equals(l.getCloudRegion()) && platform.equals(l.getCloudPlatform()));
  }

  private void constructLocationMapsRecursively(Location current) {
    if (current == null) return;

    locationsByName.put(current.getName(), current);
    HashSet<Location> currentSubLocations = new HashSet<>();

    Location[] subLocations = current.getLocations();
    // Similarly, if there are no subLocations defined in the .yml file, then this
    // field will be null rather than an empty array.
    if (subLocations != null) {
      for (Location subLocation : current.getLocations()) {
        constructLocationMapsRecursively(subLocation);
        currentSubLocations.add(subLocation);
        currentSubLocations.addAll(subLocationsWithinLocation.get(subLocation.getName()));
      }
    }

    subLocationsWithinLocation.put(current.getName(), currentSubLocations);
  }

  private List<String> extractPolicyInputLocations(PolicyInputs policyInputs) {
    List<String> result = new ArrayList<>();

    if (policyInputs != null && policyInputs.getInputs() != null) {
      Map<String, PolicyInput> inputs = policyInputs.getInputs();

      for (var key : inputs.keySet()) {
        if (key.equals(TERRA_REGION_CONSTRAINT)) {
          PolicyInput input = inputs.get(key);
          result.addAll(input.getData(TERRA_REGION_ATTRIBUTE_NAME));
        }
      }
    }

    return result;
  }

  private Set<Location> filterLocationsByCloudPlatform(Set<Location> locations, String platform) {
    if (locations == null) return Set.of();
    return locations.stream()
        .filter(l -> platform.equals(l.getCloudPlatform()))
        .collect(Collectors.toSet());
  }
}
