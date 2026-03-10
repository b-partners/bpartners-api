package app.bpartners.api.service.annotation;

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

    long targetSize = 200 * 1024; // 200 KB
    assertTrue(actual.length <= targetSize);
    BufferedImage actualBuffered = ImageIO.read(new ByteArrayInputStream(actual));
    assertNotNull(actualBuffered);
    assertTrue(actualBuffered.getWidth() <= 500);
    assertTrue(actualBuffered.getHeight() <= 500);
  }

  @Test
  void compress_image_should_respect_target_size_and_max_dimensions() throws IOException {
    BufferedImage original =
        ImageIO.read(new ClassPathResource("files/image-with-vegetation.jpg").getInputStream());

    BufferedImage actual = subject.compressImage(original);

    assertTrue(actual.getWidth() <= 500);
    assertTrue(actual.getHeight() <= 500);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(actual, "jpg", baos);
    long actualSize = baos.size();

    long targetSize = 200 * 1024; // 200 KB
    assertTrue(actualSize <= targetSize);
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
