package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactory.IMAGE_FORMAT;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

class ImageCompressorTest {

  @TempDir java.nio.file.Path tempDir;

  ImageCompressor subject = new ImageCompressor();

  @Test
  void png_image_should_preserve_alpha_channel() throws IOException {
    File original = new ClassPathResource("files/birdia_dashboard_logo.png").getFile();
    File tempOriginal = tempDir.resolve("original.png").toFile();
    Files.copy(original.toPath(), tempOriginal.toPath(), StandardCopyOption.REPLACE_EXISTING);

    File compressed = subject.compressLogoFile(tempOriginal);

    BufferedImage image = ImageIO.read(compressed);
    int pixel = image.getRGB(0, 0);
    int alpha = (pixel >> 24) & 0xff;

    assertTrue(alpha < 255);
  }

  @Test
  void compressLogoFile_should_resize_png_file() throws IOException {
    File original = new ClassPathResource("files/rue_de_la_vau.png").getFile();
    File tempOriginal = tempDir.resolve("original_resize.png").toFile();
    Files.copy(original.toPath(), tempOriginal.toPath(), StandardCopyOption.REPLACE_EXISTING);

    File compressed = subject.compressLogoFile(tempOriginal);

    assertNotNull(compressed);
    assertTrue(compressed.exists());
    assertTrue(compressed.getName().endsWith(".png"));

    BufferedImage actualBuffered = ImageIO.read(compressed);
    assertFalse(actualBuffered.getWidth() > subject.getMaxImageWidth());
    assertFalse(actualBuffered.getHeight() > subject.getMaxImageHeight());
    assertNotNull(actualBuffered);
  }

  @Test
  void compressLogoFile_should_compress_jpg_file() throws IOException {
    File original = new ClassPathResource("files/image-with-vegetation.jpg").getFile();
    File tempOriginal = tempDir.resolve("original.jpg").toFile();
    Files.copy(original.toPath(), tempOriginal.toPath(), StandardCopyOption.REPLACE_EXISTING);

    File compressed = subject.compressLogoFile(tempOriginal);

    assertNotNull(compressed);
    assertTrue(compressed.exists());
    assertTrue(compressed.getName().endsWith(".jpg"));
    assertTrue(
        compressed.length() <= tempOriginal.length()
            || compressed.getName().startsWith("converted_"));

    BufferedImage actualBuffered = ImageIO.read(compressed);
    assertNotNull(actualBuffered);
  }

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
}
