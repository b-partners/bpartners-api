package app.bpartners.api.service.annotation.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;

public final class ImageUriUtils {
  private static final String BASE_64_URI_PREFIX = "data:image/png;base64,";
  public static final String IMAGE_FORMAT = "png";

  private ImageUriUtils() {}

  public static String bufferedImageToUri(BufferedImage image) {
    return base64ToUri(base64(image));
  }

  public static String base64(BufferedImage image) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream b64 = Base64.getEncoder().wrap(out)) {
      ImageIO.write(image, IMAGE_FORMAT, b64);
      b64.flush();
      return out.toString(StandardCharsets.ISO_8859_1);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to convert image into base64 encoding " + e);
    }
  }

  public static String base64ToUri(String base64Image) {
    return !base64Image.startsWith("data:") ? BASE_64_URI_PREFIX + base64Image : base64Image;
  }
}
