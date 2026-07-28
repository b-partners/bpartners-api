package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.AreaAnnotationInstance;
import app.bpartners.api.service.annotation.AreaAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.AreaAnnotationMeasurement;
import app.bpartners.api.service.annotation.Point;
import app.bpartners.api.service.annotation.Polygon;
import app.bpartners.api.service.annotation.export.AreaAnnotationImageConf;
import app.bpartners.api.service.annotation.export.AreaAnnotationImageGenerator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.ClassPathResource;

class AreaAnnotationExportPayloadImageGeneratorTest {
  AreaAnnotationImageGenerator subject = new AreaAnnotationImageGenerator();
  AreaAnnotationImageConf conf = new AreaAnnotationImageConf();
  private static BufferedImage mockImage;
  private static final int IMAGE_SCALE = 3;

  @BeforeAll
  static void createMockImage() throws IOException {
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
  }

  @BeforeEach
  void setup() {
    // No field-level mockStatic to avoid conflicts between test classes
  }

  @Test
  void should_throw_if_bad_hexadecimal_color() {
    try (MockedStatic<ImageIO> mockedImageIo = mockStatic(ImageIO.class)) {
      mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenReturn(mockImage);

      var badExportAnnotationInstance1 = exportInstance().toBuilder().fillColor("FFFFFF").build();
      var badExportAnnotationInstance2 =
          exportInstance().toBuilder().fillColor("#FFFFFFFFFFFF").build();

      var error1 =
          assertThrows(
              BadRequestException.class,
              () -> subject.apply(mockImage, conf, List.of(badExportAnnotationInstance1)));
      var error2 =
          assertThrows(
              BadRequestException.class,
              () -> subject.apply(mockImage, conf, List.of(badExportAnnotationInstance2)));

      assertEquals("Wrong color format was received", error1.getMessage());
      assertEquals("Wrong color format was received", error2.getMessage());
    }
  }

  @Test
  void generate_image_ok() {
    try (MockedStatic<ImageIO> mockedImageIo = mockStatic(ImageIO.class)) {
      mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenReturn(mockImage);

      var expected = mockImage;
      var annotations = List.of(exportInstance());

      var actual = subject.apply(mockImage, conf, annotations);

      assertEquals(expected.getHeight() * IMAGE_SCALE, actual.getHeight());
      assertEquals(expected.getWidth() * IMAGE_SCALE, actual.getWidth());
    }
  }

  static AreaAnnotationInstance exportInstance() {
    return AreaAnnotationInstance.builder()
        .labelName("Polygon A")
        .strokeColor("#000000")
        .fillColor("#00000000")
        .infos(
            List.of(
                new AreaAnnotationInstanceInfo("Type", "Non renseigné"),
                new AreaAnnotationInstanceInfo("Surface", "305 m²")))
        .measurements(
            List.of(
                new AreaAnnotationMeasurement("m", 20d, false),
                new AreaAnnotationMeasurement("m", 20d, true),
                new AreaAnnotationMeasurement("m", 20d, true)))
        .polygon(
            new Polygon(
                List.of(
                    new Point(122, 81),
                    new Point(184, 176),
                    new Point(88, 135),
                    new Point(122, 81))))
        .build();
  }
}
