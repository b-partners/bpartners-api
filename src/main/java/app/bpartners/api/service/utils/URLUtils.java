package app.bpartners.api.service.utils;

import app.bpartners.api.model.exception.BadRequestException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class URLUtils {

  public static final int HTTP_DEFAULT_PORT = 80;
  public static final int HTTPS_DEFAULT_PORT = 443;
  public static final int PORT_NOT_SET_VALUE = -1;
  public static final String QUERY_DELIMITER = "?";

  private URLUtils() {
  }

  public static String extractURLPath(String urlString) {
    try {
      URL url = new URL(urlString);
      String protocol = url.getProtocol();
      String host = url.getHost();
      String path = url.getPath();
      int port = url.getPort();
      String portValue = port == HTTP_DEFAULT_PORT
          || port == HTTPS_DEFAULT_PORT
          || port == PORT_NOT_SET_VALUE ? ""
          : ":" + port;
      return protocol + "://" + host + portValue + path;
    } catch (MalformedURLException e) {
      throw new BadRequestException("Malformed URL : " + urlString);
    }
  }

  public static String URLEncodeMap(Map<String, String> map) {
    return map.entrySet()
        .stream()
        .map(e -> URLEncoder.encode(e.getKey(), UTF_8) + "="
            + URLEncoder.encode(e.getValue(), UTF_8))
        .collect(Collectors.joining("&"));
  }

  public static String extractURLParams(String urlString) {
    if (urlString.contains(QUERY_DELIMITER)) {
      int delimiterIndex = urlString.indexOf(QUERY_DELIMITER);
      return urlString.substring(delimiterIndex + 1);
    }
    return null;
  }
}