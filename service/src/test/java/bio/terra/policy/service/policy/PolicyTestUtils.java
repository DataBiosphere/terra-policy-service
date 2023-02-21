package bio.terra.policy.service.policy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Set;

public class PolicyTestUtils {

  protected static final String TERRA = "terra";
  protected static final String REGION_KEY = "region-name";
  protected static final String REGION_CONSTRAINT = "region-constraint";
  protected static final String GROUP_CONSTRAINT = "group-constraint";
  protected static final String GROUP_KEY = "group";
  protected static final String GROUP_NAME = "mygroup";

  protected static Multimap<String, String> buildMultimap(String key, String... values) {
    Multimap<String, String> mm = ArrayListMultimap.create();
    for (String value : values) {
      mm.put(key, value);
    }
    return mm;
  }

  protected static Multimap<String, String> buildMultimap(String key, Set<String> values) {
    Multimap<String, String> mm = ArrayListMultimap.create();
    mm.putAll(key, values);
    return mm;
  }
}
