package app.bpartners.api.service.annotation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ImageCompressorTest {

  ImageCompressor subject;

  @Test
  void compress_image() throws IOException {
    BufferedImage image =
        ImageIO.read(new ClassPathResource("files/image-with-vegetation.jpg").getInputStream());

    subject = new ImageCompressor();
  }
}
