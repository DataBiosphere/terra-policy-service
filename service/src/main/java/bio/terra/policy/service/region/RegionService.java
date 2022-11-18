package bio.terra.policy.service.region;

import bio.terra.policy.service.region.model.Datacenter;
import bio.terra.policy.service.region.model.Region;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
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
}
