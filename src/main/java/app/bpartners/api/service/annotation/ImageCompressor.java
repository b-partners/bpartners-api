package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor.IMAGE_FORMAT;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
      ImageIO.write(compressedImage, IMAGE_FORMAT, baos);

      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  BufferedImage compressImage(BufferedImage originalImage) {
    try {
      // Step 1: Determine initial quality
      long currentSize = getImageSizeBytes(originalImage);
      float quality = computeImageQuality(currentSize, imageTargetSize);

      // Step 2: Compute scale factors to fit within max width/height
      double scaleX = (double) maxImageWidth / originalImage.getWidth();
      double scaleY = (double) maxImageHeight / originalImage.getHeight();

      int targetWidth =
          (int) Math.min(Math.round(originalImage.getWidth() * scaleX), originalImage.getWidth());
      int targetHeight =
          (int) Math.min(Math.round(originalImage.getHeight() * scaleY), originalImage.getHeight());

      BufferedImage temp = originalImage;

      // Step 3: Iteratively compress until target size is reached or minimal quality
      int attempts = 0;
      while (getImageSizeBytes(temp) > imageTargetSize && quality > 0.1f && attempts++ < 10) {
        temp =
            Thumbnails.of(originalImage)
                .size(targetWidth, targetHeight)
                .outputFormat(IMAGE_FORMAT)
                .outputQuality(quality)
                .asBufferedImage();

        long newSize = getImageSizeBytes(temp);
        if (newSize >= currentSize) break;
        currentSize = newSize;

        // reduce quality slightly for next iteration
        quality *= 0.85f;
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
    ImageIO.write(image, IMAGE_FORMAT, baos);
    return baos.size();
  }

  private float computeImageQuality(long currentSize, long targetSize) {
    if (currentSize <= targetSize) {
      return 1.0f;
    }

    double ratio = (double) targetSize / currentSize;
    double quality = Math.sqrt(ratio);

    return (float) Math.clamp(quality, 0.1, 1.0);
  }
}
