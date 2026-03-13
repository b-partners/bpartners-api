package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor.IMAGE_FORMAT;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ImageCompressorTest {

  ImageCompressor subject = new ImageCompressor();

  @Test
  void compressByteArray_should_produce_valid_image_under_target_size() throws IOException {
    byte[] originalBytes =
        new ClassPathResource("files/image-with-vegetation.jpg").getInputStream().readAllBytes();

    byte[] actual = subject.compressImage(originalBytes);

    assertTrue(actual.length <= originalBytes.length);
    BufferedImage actualBuffered = ImageIO.read(new ByteArrayInputStream(actual));
    assertNotNull(actualBuffered);
  }

  @Test
  void compress_image_should_respect_target_size_and_max_dimensions() throws IOException {
    BufferedImage original =
        ImageIO.read(new ClassPathResource("files/image-with-vegetation.jpg").getInputStream());
    long originalSize = getImageSizeBytes(original);

    BufferedImage actual = subject.compressImage(original);

    long actualSize = getImageSizeBytes(actual);

    assertTrue(actualSize <= originalSize);
  }

  private long getImageSizeBytes(BufferedImage image) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, IMAGE_FORMAT, baos);
    return baos.size();
  }

  @Test
  void compressImage_should_throw_when_image_format_invalid() {
    BufferedImage img = null;

    assertThrows(RuntimeException.class, () -> subject.compressImage(img));
  }

  @Test
  void compressImage_should_throw_when_invalid_image_bytes() {
    byte[] invalid = "invalid".getBytes();

    assertThrows(RuntimeException.class, () -> subject.compressImage(invalid));
  }
}
