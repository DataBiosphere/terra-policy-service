package bio.terra.policy.service.region;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.region.model.Location;
import bio.terra.policy.service.region.model.Region;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

  // Map from a location (e.g. europe, usa, iowa) to a list of the names of all regions
  // contained in that location and sub-locations (e.g. inside usa, there are locations in
  // different states)
  private final Map<String, Set<Region>> regionsWithinLocation;
  // Map from location name to a list of the names of all sub-locations.
  private final Map<String, Set<Location>> subLocationsWithinLocation;
  // Object map from the location name to the location object.
  private final Map<String, Location> locationsByName;
  // Object map from the region name to the region object.
  private final Map<String, Region> regionsByName;

  @Autowired
  public RegionService() {
    logger.info("Loading locations from locations.yml resource.");
    Yaml locationYaml = new Yaml(new Constructor(Location.class));
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream("static/locations.yml");
    Location rootLocation = locationYaml.load(inputStream);

    /*
     * Snakeyaml can easily transform into java classes like Location above. However, it can't
     * naturally load into Java Collections. regions.yml represents an array or List of
     * regions. Since we want to work with that data type, we first create an object of that
     * type to indicate to snakeyaml how to load the file.
     */
    logger.info("Loading regions from regions.yml resource.");
    Region[] type = new Region[0];
    Yaml regionYaml = new Yaml(new Constructor(type.getClass()));
    inputStream = this.getClass().getClassLoader().getResourceAsStream("static/regions.yml");
    Region[] regions = regionYaml.load(inputStream);

    this.subLocationsWithinLocation = new HashMap<>();
    this.regionsWithinLocation = new HashMap<>();
    this.locationsByName = new HashMap<>();
    this.regionsByName = new HashMap<>();

    for (Region region : regions) {
      this.regionsByName.put(region.getId(), region);
    }

    constructLocationMapsRecursively(rootLocation);
  }

  /**
   * Get the list of all regions available in a location including subLocations, filtered by
   * platform.
   */
  @Nullable
  public Set<Region> getRegionsForLocation(String locationName, String platform) {
    String queryLocation = Strings.isNullOrEmpty(locationName) ? GLOBAL_LOCATION : locationName;
    if (!regionsWithinLocation.containsKey(queryLocation)) {
      return null;
    }
    return filterRegionsByPlatform(regionsWithinLocation.get(queryLocation), platform);
  }

  /**
   * Gets the ontology starting from the indicated location. Rather than just returning the
   * ontology, this will filter regions by the indicated platform.
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

    Set<String> regionIds =
        Optional.ofNullable(location.getRegions()).map(Set::of).orElse(Set.of());
    Set<Region> regions = regionIds.stream().map(regionsByName::get).collect(Collectors.toSet());

    Set<Region> filteredRegions = filterRegionsByPlatform(regions, platform);

    // note that this is setting the region codes into the regions field, usually these are region
    // ids
    // but the calling code expects the platform prefix to be stripped off
    result.setRegions(filteredRegions.stream().map(Region::getCode).toArray(String[]::new));

    List<Location> subLocations = new ArrayList<>();
    if (location.getLocations() != null) {
      for (Location subLocation : location.getLocations()) {
        subLocations.add(getOntology(subLocation.getName(), platform));
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

  public Set<Region> getPolicyInputRegions(PolicyInputs inputs, String platform) {
    List<String> locationNames = extractPolicyInputLocations(inputs);

    if (locationNames.isEmpty()) {
      locationNames.add(GLOBAL_LOCATION);
    }

    return locationNames.stream()
        .flatMap(
            locationName ->
                filterRegionsByPlatform(regionsWithinLocation.get(locationName), platform).stream())
        .collect(Collectors.toSet());
  }

  @Nullable
  @VisibleForTesting
  public Region getRegion(String id) {
    return regionsByName.get(id);
  }

  public boolean isRegionAllowedByPao(Pao pao, String region, String platform) {
    List<String> locationNames = extractPolicyInputLocations(pao.getEffectiveAttributes());

    if (locationNames.isEmpty()) {
      // pao doesn't have a region constraint
      return true;
    }

    String regionId = String.format("%s.%s", platform, region);
    return locationNames.stream().anyMatch(n -> locationContainsRegion(n, regionId));
  }

  public boolean isSubLocation(String parentLocationName, String subLocationName) {
    Set<Location> subLocations = subLocationsWithinLocation.get(parentLocationName);
    return subLocations != null
        && subLocations.stream().anyMatch(l -> l.getName().equals(subLocationName));
  }

  public boolean locationContainsRegion(String locationName, String regionId) {
    return regionsWithinLocation.containsKey(locationName)
        && regionsWithinLocation.get(locationName).stream()
            .anyMatch(r -> r.getId().equals(regionId));
  }

  private void constructLocationMapsRecursively(Location current) {
    if (current == null) return;

    locationsByName.put(current.getName(), current);
    HashSet<Region> currentRegions = new HashSet<>();
    HashSet<Location> currentSubLocations = new HashSet<>();

    String[] regions = current.getRegions();
    // If there are no regions defined on the location in the locations.yml file,
    // snakeyaml will set the value to null rather than populating it with an empty array.
    if (regions != null) {
      currentRegions.addAll(
          Arrays.stream(regions).map(regionsByName::get).collect(Collectors.toSet()));
    }

    Location[] subLocations = current.getLocations();
    // Similarly, if there are no subLocations defined in the .yml file, then this
    // field will be null rather than an empty array.
    if (subLocations != null) {
      for (Location subLocation : current.getLocations()) {
        constructLocationMapsRecursively(subLocation);
        currentRegions.addAll(regionsWithinLocation.get(subLocation.getName()));
        currentSubLocations.add(subLocation);
        currentSubLocations.addAll(subLocationsWithinLocation.get(subLocation.getName()));
      }
    }

    regionsWithinLocation.put(current.getName(), currentRegions);
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

  private Set<Region> filterRegionsByPlatform(Set<Region> regions, String platform) {
    return regions.stream().filter(r -> r.getId().startsWith(platform)).collect(Collectors.toSet());
  }
}
