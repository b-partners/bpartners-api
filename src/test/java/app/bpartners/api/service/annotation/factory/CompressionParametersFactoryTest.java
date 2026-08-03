package app.bpartners.api.service.annotation.factory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import app.bpartners.api.service.annotation.export.CompressionParameters;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CompressionParametersFactoryTest {

  @Test
  void compute_image_quality_should_return_one_when_current_size_is_less_than_target_size() {
    float quality = CompressionParametersFactory.computeImageQuality(500, 1000);
    assertEquals(1.0f, quality);
  }

  @Test
  void compute_image_quality_should_return_one_when_current_size_is_equal_to_target_size() {
    float quality = CompressionParametersFactory.computeImageQuality(1000, 1000);
    assertEquals(1.0f, quality);
  }

  @Test
  void
      compute_image_quality_should_return_calculated_quality_when_current_size_is_greater_than_target_size() {
    // target / current = 1/4 = 0.25. sqrt(0.25) = 0.5
    float quality = CompressionParametersFactory.computeImageQuality(400, 100);
    assertEquals(0.5f, quality);
  }

  @Test
  void compute_image_quality_should_clamp_quality_to_point_one() {
    // target / current = 1 / 10000 = 0.0001. sqrt(0.0001) = 0.01. Clamped to 0.1
    float quality = CompressionParametersFactory.computeImageQuality(10000, 1);
    assertEquals(0.1f, quality);
  }

  @Test
  void from_buffered_image_should_calculate_correct_parameters() {
    BufferedImage image = new BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB);
    // max width 50, max height 50. Scale will be min(50/100, 50/200) = min(0.5, 0.25) = 0.25
    // targetWidth = 100 * 0.25 = 25
    // targetHeight = 200 * 0.25 = 50

    CompressionParameters params = CompressionParametersFactory.from(image, "png", 1000, 50, 50);

    assertEquals(25, params.targetWidth());
    assertEquals(50, params.targetHeight());
    assertTrue(params.quality() > 0);
    assertTrue(params.originalSize() > 0);
  }

  @Test
  void from_file_should_throw_illegal_argument_exception_when_file_does_not_exist() {
    File nonExistentFile = new File("non_existent_file.png");
    assertThrows(
        IllegalArgumentException.class,
        () -> CompressionParametersFactory.from(nonExistentFile, "png", 1000, 50, 50));
  }

  @Test
  void
      from_buffered_image_should_throw_illegal_argument_exception_when_image_io_fails_to_evaluate_size() {
    BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    try (MockedStatic<ImageIO> imageIOMockedStatic = mockStatic(ImageIO.class)) {
      imageIOMockedStatic
          .when(
              () ->
                  ImageIO.write(
                      any(BufferedImage.class), anyString(), any(ByteArrayOutputStream.class)))
          .thenThrow(new IOException("Internal Error"));

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> CompressionParametersFactory.from(image, "png", 1000, 50, 50));

      assertEquals("Could not evaluate image byte size: Internal Error", exception.getMessage());
    }
  }
}
