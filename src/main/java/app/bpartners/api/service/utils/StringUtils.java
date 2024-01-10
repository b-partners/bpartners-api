package app.bpartners.api.service.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.SneakyThrows;

public class StringUtils {
  private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private StringUtils() {}

  @SneakyThrows
  public static Map<String, String> toMetadataMap(String metadataString) {
    if (metadataString == null) {
      return Map.of();
    }
    return objectMapper.readValue(metadataString, new TypeReference<>() {});
  }
}
