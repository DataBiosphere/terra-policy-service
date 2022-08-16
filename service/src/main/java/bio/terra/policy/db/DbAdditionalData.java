package bio.terra.policy.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * We use Multimap for processing additional data in the Pao processing, but we cannot use it for
 * database serdes. Instead, we use this intermediate form that is simpler to serialize.
 */
public class DbAdditionalData {
  private record DbDataPair(String key, String value) {}
  ;

  private final List<DbDataPair> dataPairList;

  @JsonCreator
  public DbAdditionalData(@JsonProperty("dataPairList") List<DbDataPair> dataPairList) {
    this.dataPairList = dataPairList;
  }

  public static Multimap<String, String> fromDb(String jsonString) {
    DbAdditionalData data = DbSerDes.fromJson(jsonString, DbAdditionalData.class);
    Multimap<String, String> mm = ArrayListMultimap.create();
    for (DbDataPair pair : data.dataPairList) {
      mm.put(pair.key(), pair.value());
    }
    return mm;
  }

  public static String toDb(Multimap<String, String> inData) {
    List<DbDataPair> dataPairList = new ArrayList<>();
    for (Map.Entry<String, String> entry : inData.entries()) {
      dataPairList.add(new DbDataPair(entry.getKey(), entry.getValue()));
    }
    DbAdditionalData dbData = new DbAdditionalData(dataPairList);
    return DbSerDes.toJson(dbData);
  }

  public List<DbDataPair> getDataPairList() {
    return dataPairList;
  }
}
