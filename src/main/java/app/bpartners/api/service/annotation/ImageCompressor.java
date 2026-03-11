package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor.IMAGE_FORMAT;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageCompressor {
  private static final int IMAGE_TARGET_SIZE = 200 * 1024; // 200 KB
  private static final int MAX_IMAGE_WIDTH = 500;
  private static final int MAX_IMAGE_HEIGHT = 500;

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
      long currentSize = getImageSizeBytes(originalImage);
      float quality = computeImageQuality(currentSize, IMAGE_TARGET_SIZE);

      BufferedImage temp =
          Thumbnails.of(originalImage)
              .size(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
              .outputFormat(IMAGE_FORMAT)
              .outputQuality(quality)
              .asBufferedImage();

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
