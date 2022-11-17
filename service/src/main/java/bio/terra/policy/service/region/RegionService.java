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
import javax.annotation.Nullable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Component
public class RegionService {
  private static final Logger logger = LoggerFactory.getLogger(RegionService.class);

  private final HashMap<String, HashSet<String>> regionDatacenterMap;
  private final HashMap<String, Region> regionNameMap;
  private final HashMap<String, Datacenter> datacenterNameMap;

  @Autowired
  public RegionService() {
    Yaml regionYaml = new Yaml(new Constructor(Region.class));
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream("static/regions.yml");
    Region rootRegion = regionYaml.load(inputStream);

    /**
     * Snakeyaml can easily transform into java classes like Regions above. However, it can't
     * naturally load into Java Collections. Datacenters.yml represents an array or List of
     * Datacenters. Since we want to work with that data type, we first create an object of that
     * type to indicate to snakeyaml how to load the file.
     */
    Datacenter[] type = new Datacenter[0];
    Yaml datacenterYaml = new Yaml(new Constructor(type.getClass()));
    inputStream = this.getClass().getClassLoader().getResourceAsStream("static/datacenters.yml");
    Datacenter[] datacenters = datacenterYaml.load(inputStream);

    this.regionDatacenterMap = new HashMap<>();
    this.regionNameMap = new HashMap<>();
    this.datacenterNameMap = new HashMap();

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

  @Nullable
  public Datacenter getDatacenter(String id) {
    return datacenterNameMap.get(id);
  }

  public Boolean paoContainsDatacenter(Pao pao, String datacenter) {
    List<String> regionNames = extractPolicyRegions(pao);

    if (regionNames.isEmpty()) {
      // pao doesn't have a region policy
      return true;
    }

    for (String regionName : regionNames) {
      if (regionContainsDatacenter(regionName, datacenter)) {
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
        if (key.equals("terra:region-constraint")) {
          PolicyInput input = inputs.get(key);
          result.addAll(input.getData("region"));
        }
      }
    }

    return result;
  }
}
