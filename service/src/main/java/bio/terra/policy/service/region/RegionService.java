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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  private static final String GLOBAL = "global";

  private static final Logger logger = LoggerFactory.getLogger(RegionService.class);

  // Map from a geographic location (e.g. europe, usa, iowa) to a list of the names of all regions
  // contained in that location and sub-locations (e.g. inside usa, there are data centers in
  // different states)
  private final HashMap<String, HashSet<String>> locationMap;
  // Map from location geographic name to a list of the names of all sub-locations.
  private final HashMap<String, HashSet<String>> locationSubLocationsMap;
  // Object map from the location name to the location object.
  private final HashMap<String, Location> locationNameMap;
  // Object map from the region name to the region object.
  private final HashMap<String, Region> regionNameMap;

  @Autowired
  public RegionService() {
    logger.info("Loading regions from locations.yml resource.");
    Yaml regionYaml = new Yaml(new Constructor(Location.class));
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream("static/locations.yml");
    Location rootRegion = regionYaml.load(inputStream);

    /*
     * Snakeyaml can easily transform into java classes like Regions above. However, it can't
     * naturally load into Java Collections. Datacenters.yml represents an array or List of
     * Datacenters. Since we want to work with that data type, we first create an object of that
     * type to indicate to snakeyaml how to load the file.
     */
    logger.info("Loading regions from regions.yml resource.");
    Region[] type = new Region[0];
    Yaml datacenterYaml = new Yaml(new Constructor(type.getClass()));
    inputStream = this.getClass().getClassLoader().getResourceAsStream("static/regions.yml");
    Region[] regions = datacenterYaml.load(inputStream);

    this.locationSubLocationsMap = new HashMap<>();
    this.locationMap = new HashMap<>();
    this.locationNameMap = new HashMap<>();
    this.regionNameMap = new HashMap<>();

    for (Region region : regions) {
      this.regionNameMap.put(region.getId(), region);
    }

    constructRegionMapsRecursively(rootRegion);
  }

  /**
   * Get the list of all datacenters available in a region and its subregions, filtered by platform.
   */
  @Nullable
  public HashSet<String> getRegionsForLocation(String geographicLocation, String platform) {
    String queryRegion = Strings.isNullOrEmpty(geographicLocation) ? GLOBAL : geographicLocation;
    HashSet<String> result = new HashSet<>();
    HashSet<String> regionDataCenters = locationMap.get(queryRegion);
    result.addAll(filterRegionsByPlatform(regionDataCenters, platform));
    return result;
  }

  /**
   * Gets the ontology starting from the indicated region. Rather than just returning the ontology,
   * this will filter data centers by the indicated platform.
   */
  @Nullable
  public Location getOntology(String regionName, String platform) {
    String queryRegion = Strings.isNullOrEmpty(regionName) ? GLOBAL : regionName;
    Location mappedRegion = locationNameMap.get(queryRegion);

    if (mappedRegion == null) {
      return null;
    }

    Location result = new Location();
    result.setName(mappedRegion.getName());
    result.setDescription(mappedRegion.getDescription());

    String[] regions = mappedRegion.getRegions();

    if (regions == null) {
      regions = new String[0];
    }

    List<String> filteredDatacenters = filterRegionsByPlatform(Arrays.asList(regions), platform);

    result.setRegions(filteredDatacenters.toArray(new String[0]));

    List<Location> subregions = new ArrayList<>();
    if (mappedRegion.getLocations() != null) {
      for (Location subregion : mappedRegion.getLocations()) {
        subregions.add(getOntology(subregion.getName(), platform));
      }
    }
    result.setLocations(subregions.toArray(new Location[0]));

    return result;
  }

  @Nullable
  @VisibleForTesting
  public Location getLocation(String name) {
    return locationNameMap.get(name);
  }

  public HashSet<String> getPolicyInputRegionCodes(PolicyInputs inputs, String platform) {
    List<String> regionNames = extractPolicyInputLocations(inputs);
    HashSet<String> result = new HashSet<>();

    if (regionNames.isEmpty()) {
      regionNames.add(GLOBAL);
    }

    for (String regionName : regionNames) {
      result.addAll(filterRegionsByPlatform(locationMap.get(regionName), platform));
    }

    return result;
  }

  @Nullable
  @VisibleForTesting
  public Region getRegion(String id) {
    return regionNameMap.get(id);
  }

  public boolean isRegionAllowedByPao(Pao pao, String region, String platform) {
    List<String> regionNames = extractPolicyInputLocations(pao.getEffectiveAttributes());

    if (regionNames.isEmpty()) {
      // pao doesn't have a region constraint
      return true;
    }

    String tpsRegion = String.format("%s.%s", platform, region);
    for (String regionName : regionNames) {
      if (locationContainsRegion(regionName, tpsRegion)) {
        return true;
      }
    }

    return false;
  }

  public boolean isSubLocation(String parentLocationName, String subLocationName) {
    HashSet<String> subLocations = locationSubLocationsMap.get(parentLocationName);
    return subLocations != null && subLocations.contains(subLocationName);
  }

  public boolean locationContainsRegion(String locationName, String region) {
    return locationMap.containsKey(locationName) && locationMap.get(locationName).contains(region);
  }

  private void constructRegionMapsRecursively(Location current) {
    if (current == null) return;

    locationNameMap.put(current.getName(), current);
    HashSet<String> currentRegions = new HashSet<>();
    HashSet<String> currentSubLocations = new HashSet<>();

    String[] regions = current.getRegions();
    // If there are no regions defined on the location in the locations.yml file,
    // snakeyaml will set the value to null rather than populating it with an empty array.
    if (regions != null) {
      currentRegions.addAll(List.of(regions));
    }

    Location[] subregions = current.getLocations();
    // Similarly, if there are no subregions defined in the .yml file, then this
    // field will be null rather than an empty array.
    if (subregions != null) {
      for (Location subregion : current.getLocations()) {
        constructRegionMapsRecursively(subregion);
        currentRegions.addAll(locationMap.get(subregion.getName()));
        currentSubLocations.add(subregion.getName());
        currentSubLocations.addAll(locationSubLocationsMap.get(subregion.getName()));
      }
    }

    locationMap.put(current.getName(), currentRegions);
    locationSubLocationsMap.put(current.getName(), currentSubLocations);
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

  private List<String> filterRegionsByPlatform(Collection<String> regions, String platform) {
    List<String> result = new ArrayList<>();

    if (regions == null) {
      return result;
    }

    for (String datacenterId : regions) {
      if (datacenterId.startsWith(platform)) {
        result.add(regionNameMap.get(datacenterId).getCode());
      }
    }

    return result;
  }
}
