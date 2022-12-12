package app.bpartners.api.service.utils;

import java.util.Base64;

public class FileUtils {
  private FileUtils() {
  }

  public static String base64Image(byte[] image) {
    if (image == null) {
      return null;
    }
    return Base64.getEncoder().encodeToString(image);
  }
}
