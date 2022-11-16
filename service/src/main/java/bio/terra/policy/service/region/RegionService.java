package bio.terra.policy.service.region;

import bio.terra.policy.service.region.model.Datacenter;
import bio.terra.policy.service.region.model.Region;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Component
public class RegionService {
  private static final Logger logger = LoggerFactory.getLogger(RegionService.class);

  private final Region ontology;

  private final HashMap<String, HashSet<String>> regionDatacenterMap;
  private final HashMap<String, Region> regionNameMap;
  private final HashMap<String, Datacenter> datacenterNameMap;

  @Autowired
  public RegionService() {
    Yaml regionYaml = new Yaml(new Constructor(Region.class));
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream("static/regions.yml");
    this.ontology = regionYaml.load(inputStream);

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

    ConstructRegionMapsRecursively(this.ontology);
  }

  public Region getRegion(String name) {
    return regionNameMap.containsKey(name) ? regionNameMap.get(name) : null;
  }

  public Datacenter getDatacenter(String id) {
    return this.datacenterNameMap.containsKey(id) ? datacenterNameMap.get(id) : null;
  }

  private void ConstructRegionMapsRecursively(Region current) {
    if (current == null) return;

    regionNameMap.put(current.getName(), current);

    HashSet<String> currentDatacenters = new HashSet<>();

    String[] datacenters = current.getDatacenters();
    if (datacenters != null) {
      currentDatacenters.addAll(List.of(datacenters));
    }

    Region[] subregions = current.getRegions();
    if (subregions != null) {
      for (Region subregion : current.getRegions()) {
        ConstructRegionMapsRecursively(subregion);
        currentDatacenters.addAll(regionDatacenterMap.get(subregion.getName()));
      }
    }

    regionDatacenterMap.put(current.getName(), currentDatacenters);
  }
}
