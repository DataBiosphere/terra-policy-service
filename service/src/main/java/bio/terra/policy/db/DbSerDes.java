package bio.terra.policy.db;

import bio.terra.common.exception.SerializationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.collect.Multimap;

/**
 * Object mapper for use in the DAO modules. This mapper must stay constant over time to ensure that
 * older versions of obvious can be read. Change here must be accompanied by an upgrade process to
 * ensure that all data is rewritten in the new form.
 */
public class DbSerDes {
  private static final ObjectMapper serdesMapper =
      new ObjectMapper()
          .registerModule(new ParameterNamesModule())
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .setDefaultPropertyInclusion(JsonInclude.Include.NON_ABSENT);

  public static String toJson(Object value) {
    try {
      return serdesMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new SerializationException("Failed toJson", e);
    }
  }

  public static <T> T fromJson(String json, Class<T> classType) {
    try {
      return serdesMapper.readValue(json, classType);
    } catch (JsonProcessingException e) {
      throw new SerializationException("Failed fromJson", e);
    }
  }

  // Specific mappers for key-value properties
  public static String propertiesToJson(Multimap<String, String> kvmap) {
    return toJson(kvmap);
  }

  public static Multimap<String, String> jsonToProperties(String json) {
    try {
      return serdesMapper.readValue(json, new TypeReference<Multimap<String, String>>() {});
    } catch (JsonProcessingException ex) {
      throw new SerializationException("Failed to deserialize properties", ex);
    }
  }
}
