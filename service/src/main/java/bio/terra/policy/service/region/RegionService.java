package bio.terra.policy.service.region;

import bio.terra.policy.common.model.PolicyInput;
import bio.terra.policy.common.model.PolicyInputs;
import bio.terra.policy.service.pao.model.Pao;
import bio.terra.policy.service.region.model.Datacenter;
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
  private static final String GLOBAL_REGION = "global";

  private static final Logger logger = LoggerFactory.getLogger(RegionService.class);

  private final HashMap<String, HashSet<String>> regionDatacenterMap;
  private final HashMap<String, Region> regionNameMap;
  private final HashMap<String, Datacenter> datacenterNameMap;

  @Autowired
  public RegionService() {
    logger.info("Loading regions from regions.yml resource.");
    Yaml regionYaml = new Yaml(new Constructor(Region.class));
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream("static/regions.yml");
    Region rootRegion = regionYaml.load(inputStream);

    /*
     * Snakeyaml can easily transform into java classes like Regions above. However, it can't
     * naturally load into Java Collections. Datacenters.yml represents an array or List of
     * Datacenters. Since we want to work with that data type, we first create an object of that
     * type to indicate to snakeyaml how to load the file.
     */
    logger.info("Loading datacenters from datacenters.yml resource.");
    Datacenter[] type = new Datacenter[0];
    Yaml datacenterYaml = new Yaml(new Constructor(type.getClass()));
    inputStream = this.getClass().getClassLoader().getResourceAsStream("static/datacenters.yml");
    Datacenter[] datacenters = datacenterYaml.load(inputStream);

    this.regionDatacenterMap = new HashMap<>();
    this.regionNameMap = new HashMap<>();
    this.datacenterNameMap = new HashMap<>();

    for (Datacenter datacenter : datacenters) {
      this.datacenterNameMap.put(datacenter.getId(), datacenter);
    }

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

  /**
   * Read through the PAO and its region constraints to determine which data centers should be
   * available.
   *
   * @param pao The PAO to scan.
   * @param platform The platform for the PAO - IE: gcp | azure
   * @return the set of datacenter codes allowed by the PAO. Codes do not include the platform
   *     prefix.
   */
  public HashSet<String> getPaoDatacenterCodes(Pao pao, String platform) {
    List<String> regionNames = extractPolicyRegions(pao);
    HashSet<String> result = new HashSet<>();

    if (regionNames.isEmpty()) {
      regionNames.add(GLOBAL_REGION);
    }

    for (String regionName : regionNames) {
      HashSet<String> datacenterIds = regionDatacenterMap.get(regionName);
      if (datacenterIds != null) {
        for (String datacenterId : datacenterIds) {
          if (datacenterId.startsWith(platform)) {
            result.add(datacenterNameMap.get(datacenterId).getCode());
          }
        }
      }
    }

    return result;
  }

  @Nullable
  public Datacenter getDatacenter(String id) {
    return datacenterNameMap.get(id);
  }

  public boolean isDatacenterAllowedByPao(Pao pao, String datacenter, String platform) {
    List<String> regionNames = extractPolicyRegions(pao);

    if (regionNames.isEmpty()) {
      // pao doesn't have a region constraint
      return true;
    }

    String tpsDatacenter = String.format("%s.%s", platform, datacenter);
    for (String regionName : regionNames) {
      if (regionContainsDatacenter(regionName, tpsDatacenter)) {
        return true;
      }
    }

    return false;
  }

  private void constructRegionMapsRecursively(Region current) {
    if (current == null) return;

    regionNameMap.put(current.getName(), current);
    HashSet<String> currentDatacenters = new HashSet<>();
    String[] datacenters = current.getDatacenters();

    // If there are no datacenters defined on the region in the regions.yml file,
    // snakeyaml will set the value to null rather than populating it with an empty array.
    if (datacenters != null) {
      currentDatacenters.addAll(List.of(datacenters));
    }

    Region[] subregions = current.getRegions();

    // Similarly, if there are no subregions defined in the .yml file, then this
    // field will be null rather than an empty array.
    if (subregions != null) {
      for (Region subregion : current.getRegions()) {
        constructRegionMapsRecursively(subregion);
        currentDatacenters.addAll(regionDatacenterMap.get(subregion.getName()));
      }
    }

    regionDatacenterMap.put(current.getName(), currentDatacenters);
  }

  private List<String> extractPolicyRegions(Pao pao) {
    List<String> result = new ArrayList<>();
    PolicyInputs effectiveAttributes = pao.getEffectiveAttributes();

    if (effectiveAttributes != null && effectiveAttributes.getInputs() != null) {
      Map<String, PolicyInput> inputs = effectiveAttributes.getInputs();

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
