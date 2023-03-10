package app.bpartners.api.service.utils;

import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class URLUtils {
  private URLUtils() {
  }

  public static String URLEncodeMap(Map<String, String> map) {
    return map.entrySet()
        .stream()
        .map(e -> URLEncoder.encode(e.getKey(), UTF_8) + "="
            + URLEncoder.encode(e.getValue(), UTF_8))
        .collect(Collectors.joining("&"));
  }
}