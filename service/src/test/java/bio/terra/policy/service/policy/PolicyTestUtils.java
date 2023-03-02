package bio.terra.policy.service.policy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Set;

public class PolicyTestUtils {
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
