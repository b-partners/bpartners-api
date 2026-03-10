package app.bpartners.api.service.annotation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ImageCompressorTest {

  ImageCompressor subject = new ImageCompressor();

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
}
