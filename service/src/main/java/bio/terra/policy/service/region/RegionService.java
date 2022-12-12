package bio.terra.policy.service.region;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.region.model.Region;
import java.io.InputStream;
import java.util.ArrayList;
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

  private static final Logger logger = LoggerFactory.getLogger(RegionService.class);

  // Map from a region name to a list of the names of all data centers contained in that region and
  // its subregions.
  private final HashMap<String, HashSet<String>> regionDatacenterMap;
  // Map from region name to a list of the names of all subregions contained in that region.
  private final HashMap<String, HashSet<String>> regionSubregionMap;
  // Object map from the region name to the region object.
  private final HashMap<String, Region> regionNameMap;

  @Autowired
  public RegionService() {
    logger.info("Loading regions from regions.yml resource.");
    Yaml regionYaml = new Yaml(new Constructor(Region.class));
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream("static/regions.yml");
    Region rootRegion = regionYaml.load(inputStream);

    this.regionSubregionMap = new HashMap<>();
    this.regionDatacenterMap = new HashMap<>();
    this.regionNameMap = new HashMap<>();

    constructRegionMapsRecursively(rootRegion);
  }

  public boolean regionContainsDatacenter(String regionName, String datacenterId) {
    return regionDatacenterMap.containsKey(regionName)
        && regionDatacenterMap.get(regionName).contains(datacenterId);
  }

  @Nullable
  public Region getRegion(String name) {
    return regionNameMap.get(name);
  }

  public HashSet<String> getPolicyInputDataCenterCodes(PolicyInputs inputs, String platform) {
    List<String> regionNames = extractPolicyInputRegions(inputs);
    HashSet<String> result = new HashSet<>();

    if (regionNames.isEmpty()) {
      return regionDatacenterMap.get(platform);
    }

    var platformRegions = regionSubregionMap.get(platform);

    for (String regionName : regionNames) {
      if (platformRegions.contains(regionName)) {
        HashSet<String> datacenterIds = regionDatacenterMap.get(regionName);
        result.addAll(datacenterIds);
      }
    }

    return result;
  }

  public boolean isDatacenterAllowedByPao(Pao pao, String datacenter, String platform) {
    List<String> regionNames = extractPolicyInputRegions(pao.getEffectiveAttributes());

    if (regionNames.isEmpty()) {
      // pao doesn't have a region constraint
      return true;
    }

    HashSet<String> platformDatacenters = regionDatacenterMap.get(platform);

    for (String regionName : regionNames) {
      if (platformDatacenters.contains(datacenter)
          && regionContainsDatacenter(regionName, datacenter)) {
        return true;
      }
    }

    return false;
  }

  public boolean isSubregion(String parentRegionName, String subregionName) {
    HashSet<String> subregions = regionSubregionMap.get(parentRegionName);
    return (subregions == null) ? false : subregions.contains(subregionName);
  }

  /**
   * DFS approach. Leaf nodes are equal to individual data centers and can be added to the map. When
   * we've calculated all children, we can map the current node to its subregions and data centers.
   */
  private void constructRegionMapsRecursively(Region current) {
    regionNameMap.put(current.getName(), current);
    HashSet<String> currentDatacenters = new HashSet<>();
    HashSet<String> currentSubregions = new HashSet<>();

    Region[] subregions = current.getRegions();
    // If there are no subregions defined in the .yml file, then this
    // field will be null rather than an empty array.
    if (subregions == null) {
      // If we have no subregions, we have hit a leaf node and the current region
      // represents a single data center region. Don't recurse further.
      currentDatacenters.add(current.getName());
    } else {
      for (Region subregion : current.getRegions()) {
        constructRegionMapsRecursively(subregion);
        currentDatacenters.addAll(regionDatacenterMap.get(subregion.getName()));
        currentSubregions.add(subregion.getName());
        currentSubregions.addAll(regionSubregionMap.get(subregion.getName()));
      }
    }

    regionDatacenterMap.put(current.getName(), currentDatacenters);
    regionSubregionMap.put(current.getName(), currentSubregions);
  }

  private List<String> extractPolicyInputRegions(PolicyInputs policyInputs) {
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
}
