package app.bpartners.api.service.utils;

import java.net.URLConnection;
import org.springframework.http.MediaType;

public class FileInfoUtils {
  public static MediaType parseMediaType(String fileName) {
    return MediaType.parseMediaType(URLConnection.guessContentTypeFromName(fileName));
  }
}