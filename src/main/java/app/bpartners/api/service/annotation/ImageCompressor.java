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
      long targetSize = 200 * 1024; // 200 KB
      float quality = computeImageQuality(currentSize, targetSize);

      BufferedImage temp =
          Thumbnails.of(originalImage)
              .size(500, 500)
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

    return (float) Math.max(0.1, Math.min(quality, 1.0));
  }
}
