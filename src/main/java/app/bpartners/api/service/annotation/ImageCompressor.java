package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.utils.ImageUriUtils.toJpegCompatible;

import app.bpartners.api.service.annotation.factory.CompressionParametersFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
@Slf4j
public class ImageCompressor {
  private static final int DEFAULT_IMAGE_TARGET_SIZE = 200 * 1024; // 200 KB
  private static final int DEFAULT_MAX_IMAGE_WIDTH = 1180;
  private static final int DEFAULT_MAX_IMAGE_HEIGHT = 1180;
  private static final String IMAGE_COMPRESSION_FORMAT = "jpg";
  public static final float QUALITY_DECREASE_RATE = 0.85f;

  private final int imageTargetSize;
  private final int maxImageWidth;
  private final int maxImageHeight;

  public ImageCompressor() {
    this(DEFAULT_IMAGE_TARGET_SIZE, DEFAULT_MAX_IMAGE_WIDTH, DEFAULT_MAX_IMAGE_HEIGHT);
  }

  byte[] compressImage(byte[] originalImage) {
    try {
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalImage));
      BufferedImage compressedImage = compressImage(image);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(compressedImage, IMAGE_COMPRESSION_FORMAT, baos);

      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public File compressLogoFile(File logoFile) {
    if (isPNGFile(logoFile)) {
      return compressPNGFile(logoFile);
    }
    return compressImage(logoFile);
  }

  private File compressPNGFile(File pngFile) {
    CompressionParameters params;
    try {
      params = getCompressionParameters(pngFile);
    } catch (IllegalArgumentException e) {
      log.warn(
          "Unable to compress PNG file {}, keeping original. {}",
          pngFile.getName(),
          e.getMessage());
      return pngFile;
    }

    try {
      Thumbnails.of(pngFile).size(params.targetWidth(), params.targetHeight()).toFile(pngFile);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Unable to write compressed PNG file: " + e.getMessage(), e);
    }

    return pngFile;
  }

  public BufferedImage compressImage(BufferedImage originalImage) {
    CompressionParameters params;
    try {
      params =
          CompressionParametersFactory.from(
              originalImage,
              IMAGE_COMPRESSION_FORMAT,
              imageTargetSize,
              maxImageWidth,
              maxImageHeight);
    } catch (IllegalArgumentException e) {
      log.warn(
          "Unable to compute compression parameters, keeping original image. {}", e.getMessage());
      return originalImage;
    }

    try {
      int attempts = 0;
      long currentSize = params.originalSize();
      float currentQuality = params.quality();
      BufferedImage temp = originalImage;
      while (getImageSizeBytes(temp) > imageTargetSize
          && currentQuality > 0.1f
          && attempts++ < 10) {
        temp =
            Thumbnails.of(originalImage)
                .size(params.targetWidth(), params.targetHeight())
                .outputFormat(IMAGE_COMPRESSION_FORMAT)
                .outputQuality(currentQuality)
                .asBufferedImage();

        long newSize = getImageSizeBytes(temp);
        if (newSize >= currentSize) break;

        currentSize = newSize;
        currentQuality *= QUALITY_DECREASE_RATE;
      }

      return convertToJPEGCompatibleType(temp);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File compressImage(File originalImage) {
    CompressionParameters params;
    try {
      params = getCompressionParameters(originalImage);
    } catch (IllegalArgumentException e) {
      log.warn(
          "Unable to compute compression parameters for {}, keeping original image. {}",
          originalImage.getName(),
          e.getMessage());
      return originalImage;
    }

    try {
      int attempts = 0;
      long currentSize = params.originalSize();
      float currentQuality = params.quality();
      File temp = originalImage;
      while (temp.length() > imageTargetSize && currentQuality > 0.1f && attempts++ < 10) {
        Thumbnails.of(originalImage)
            .size(params.targetWidth(), params.targetHeight())
            .outputFormat(IMAGE_COMPRESSION_FORMAT)
            .outputQuality(currentQuality)
            .toFile(temp);

        long newSize = temp.length();
        if (newSize >= currentSize) break;

        currentSize = newSize;
        currentQuality *= QUALITY_DECREASE_RATE;
      }

      return convertToJPEGCompatibleType(temp);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private CompressionParameters getCompressionParameters(File originalImage) {
    return CompressionParametersFactory.from(
        originalImage, IMAGE_COMPRESSION_FORMAT, imageTargetSize, maxImageWidth, maxImageHeight);
  }

  private boolean isPNGFile(File file) {
    return file.getName().toLowerCase().endsWith(".png");
  }

  private File convertToJPEGCompatibleType(File image) throws IOException {
    BufferedImage bufferedImage = ImageIO.read(image);
    BufferedImage compatible = convertToJPEGCompatibleType(bufferedImage);
    File outputFile = new File(image.getParent(), "converted_" + image.getName() + ".jpg");
    ImageIO.write(compatible, "jpg", outputFile);

    return outputFile;
  }

  private BufferedImage convertToJPEGCompatibleType(BufferedImage image) {
    return toJpegCompatible(image);
  }

  private long getImageSizeBytes(BufferedImage image) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, IMAGE_COMPRESSION_FORMAT, baos);
    return baos.size();
  }
}
