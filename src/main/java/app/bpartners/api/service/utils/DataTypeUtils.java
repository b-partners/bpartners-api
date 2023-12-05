package app.bpartners.api.service.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.SneakyThrows;

public class DataTypeUtils {
  static final ObjectMapper objectMapper = new ObjectMapper();

  private DataTypeUtils() {

  }

  @SneakyThrows
  public static String encodeJsonList(List<String> values) {
    return objectMapper.writeValueAsString(values);
  }

  @SneakyThrows
  public static List<String> decodeJsonList(String value) {
    return objectMapper.readValue(value, new TypeReference<>() {
    });
  }
}
