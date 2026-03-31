package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.service.annotation.CompressionParameters;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class CompressionParametersFactory {

  private CompressionParametersFactory() {}

  public static float computeImageQuality(long currentSize, long targetSize) {
    if (currentSize <= targetSize) {
      return 1.0f;
    }

    double ratio = (double) targetSize / currentSize;
    double quality = Math.sqrt(ratio);

    return (float) Math.clamp(quality, 0.1, 1.0);
  }

  public static CompressionParameters from(
      File originalImage,
      String imageFormat,
      int imageTargetSize,
      int maxImageWidth,
      int maxImageHeight)
      throws IOException {
    BufferedImage bufferedImage = ImageIO.read(originalImage);
    return from(bufferedImage, imageFormat, imageTargetSize, maxImageWidth, maxImageHeight);
  }

  public static CompressionParameters from(
      BufferedImage image,
      String imageFormat,
      int imageTargetSize,
      int maxImageWidth,
      int maxImageHeight)
      throws IOException {
    long originalSize = computeImageSizeBytes(image, imageFormat);
    float quality = computeImageQuality(originalSize, imageTargetSize);

    double scaleX = (double) maxImageWidth / image.getWidth();
    double scaleY = (double) maxImageHeight / image.getHeight();

    int targetWidth = (int) Math.min(Math.round(image.getWidth() * scaleX), image.getWidth());
    int targetHeight = (int) Math.min(Math.round(image.getHeight() * scaleY), image.getHeight());

    return new CompressionParameters(originalSize, targetWidth, targetHeight, quality);
  }

  private static long computeImageSizeBytes(BufferedImage image, String imageFormat)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, imageFormat, baos);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Given file is unconvertible into " + imageFormat, e);
    }
    return baos.size();
  }
}
