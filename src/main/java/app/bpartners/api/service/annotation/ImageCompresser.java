package app.bpartners.api.service.annotation;

import net.coobird.thumbnailator.Thumbnails;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageCompresser {
  static BufferedImage compressImage(BufferedImage originalImage) {
    try {
      long currentSize = originalImage.getPropertyNames().length;
      long targetSize = 500 * 500;

      float quality = computeImageQuality(currentSize, targetSize);

      return Thumbnails.of(originalImage).size(500, 500).outputQuality(quality).asBufferedImage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static float computeImageQuality(long currentSize, long targetSize) {
    if (currentSize <= targetSize) {
      return 1.0f;
    }

    double ratio = (double) targetSize / currentSize;
    double quality = Math.sqrt(ratio);

    return (float) Math.max(0.1, Math.min(quality, 1.0));
  }
}
