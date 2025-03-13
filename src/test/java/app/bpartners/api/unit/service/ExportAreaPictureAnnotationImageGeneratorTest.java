package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageConf;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageGenerator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.ClassPathResource;

class ExportAreaPictureAnnotationImageGeneratorTest {
  ExportAreaPictureAnnotationImageGenerator subject =
      new ExportAreaPictureAnnotationImageGenerator();
  ExportAreaPictureAnnotationImageConf conf = new ExportAreaPictureAnnotationImageConf();
  MockedStatic<ImageIO> mockedImageIo = mockStatic(ImageIO.class);
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
    mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenReturn(mockImage);
  }

  @AfterEach
  void cleanup() {
    mockedImageIo.close();
  }

  @Test
  void should_throw_if_bad_hexadecimal_color() {
    var badExportAnnotationInstance1 = exportAreaPictureAnnotationInstance().fillColor("FFFFFF");
    var badExportAnnotationInstance2 =
        exportAreaPictureAnnotationInstance().fillColor("#FFFFFFFFFFFF");
    var exportAreaPictureAnnotation1 =
        exportAreaPictureAnnotation().annotations(List.of(badExportAnnotationInstance1));
    var exportAreaPictureAnnotation2 =
        exportAreaPictureAnnotation().annotations(List.of(badExportAnnotationInstance2));
    var annotations1 = exportAreaPictureAnnotation1.getAnnotations();
    var annotations2 = exportAreaPictureAnnotation2.getAnnotations();

    var error1 =
        assertThrows(BadRequestException.class, () -> subject.apply(mockImage, conf, annotations1));
    var error2 =
        assertThrows(BadRequestException.class, () -> subject.apply(mockImage, conf, annotations2));

    assertEquals("Wrong color format was received", error1.getMessage());
    assertEquals("Wrong color format was received", error2.getMessage());
  }

  @Test
  void generate_image_ok() {
    var expected = mockImage;
    var annotations = exportAreaPictureAnnotation().getAnnotations();

    var actual = subject.apply(mockImage, conf, annotations);

    assertEquals(expected.getHeight() * IMAGE_SCALE, actual.getHeight());
    assertEquals(expected.getWidth() * IMAGE_SCALE, actual.getWidth());
  }

  static ExportAreaPictureAnnotation exportAreaPictureAnnotation() {
    return new ExportAreaPictureAnnotation()
        .imageUrl("https://dummy.com")
        .annotations(List.of(exportAreaPictureAnnotationInstance()));
  }

  static ExportAreaPictureAnnotationInstance exportAreaPictureAnnotationInstance() {
    return new ExportAreaPictureAnnotationInstance()
        .labelName("Polygon A")
        .strokeColor("#000000")
        .fillColor("#00000000")
        .infos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo().label("Type").value("Non renseigné"),
                new ExportAreaPictureAnnotationInstanceInfo().label("Surface").value("305 m²")))
        .measurements(
            List.of(
                new ExportAreaPictureAnnotationMeasurement()
                    .value(20d)
                    .isInvisible(false)
                    .unit("m"),
                new ExportAreaPictureAnnotationMeasurement().value(20d).isInvisible(true).unit("m"),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(20d)
                    .isInvisible(true)
                    .unit("m")))
        .polygon(
            new Polygon()
                .points(
                    List.of(
                        new Point().x(122d).y(81d),
                        new Point().x(184d).y(176d),
                        new Point().x(88d).y(135d),
                        new Point().x(122d).y(81d))));
  }
}
