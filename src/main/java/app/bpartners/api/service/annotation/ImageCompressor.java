package app.bpartners.api.service.annotation;

import app.bpartners.api.service.annotation.factory.CompressionParametersFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageCompressor {
  private static final int DEFAULT_IMAGE_TARGET_SIZE = 200 * 1024; // 200 KB
  private static final int DEFAULT_MAX_IMAGE_WIDTH = 1180;
  private static final int DEFAULT_MAX_IMAGE_HEIGHT = 1180;
  private static final String IMAGE_COMPRESSION_FORMAT = "jpg";
  public static final float QUALITY_DECREASE_RATE = 0.85f;
  public static final String LOGO_COMPRESSION_FORMAT = "png";

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

  public File compressPNGImage(File originalImage) {
    try {
      CompressionParameters params =
          CompressionParametersFactory.from(
              originalImage,
              LOGO_COMPRESSION_FORMAT,
              imageTargetSize,
              maxImageWidth,
              maxImageHeight);

      int attempts = 0;
      long currentSize = params.originalSize();
      float currentQuality = params.quality();
      File temp = originalImage;
      while (temp.length() > imageTargetSize && currentQuality > 0.1f && attempts++ < 10) {
        Thumbnails.of(temp)
            .size(params.targetWidth(), params.targetHeight())
            .imageType(BufferedImage.TYPE_INT_ARGB)
            .outputFormat(LOGO_COMPRESSION_FORMAT)
            .toFile(temp);

        long newSize = temp.length();
        if (newSize >= currentSize) break;

        currentSize = newSize;
        currentQuality *= QUALITY_DECREASE_RATE;
      }

      return temp;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public BufferedImage compressImage(BufferedImage originalImage) {
    try {
      CompressionParameters params =
          CompressionParametersFactory.from(
              originalImage,
              IMAGE_COMPRESSION_FORMAT,
              imageTargetSize,
              maxImageWidth,
              maxImageHeight);

      int attempts = 0;
      long currentSize = params.originalSize();
      float currentQuality = params.quality();
      BufferedImage temp = originalImage;
      while (getImageSizeBytes(temp) > imageTargetSize
          && currentQuality > 0.1f
          && attempts++ < 10) {
        temp =
            Thumbnails.of(temp)
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

  private BufferedImage convertToJPEGCompatibleType(BufferedImage image) {
    BufferedImage compatible =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
    compatible.getGraphics().drawImage(image, 0, 0, null);

    return compatible;
  }

  private long getImageSizeBytes(BufferedImage image) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, IMAGE_COMPRESSION_FORMAT, baos);
    return baos.size();
  }
}
