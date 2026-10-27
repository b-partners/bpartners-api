package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageConf;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageGenerator;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationMeasurement;
import app.bpartners.api.service.annotation.model.export.Point;
import app.bpartners.api.service.annotation.model.export.Polygon;
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
    var badExportAnnotationInstance1 = exportAreaPictureAnnotationInstance().setFillColor("FFFFFF");
    var badExportAnnotationInstance2 =
        exportAreaPictureAnnotationInstance().setFillColor("#FFFFFFFFFFFF");
    var exportAreaPictureAnnotation1 =
        exportAreaPictureAnnotation().setAnnotations(List.of(badExportAnnotationInstance1));
    var exportAreaPictureAnnotation2 =
        exportAreaPictureAnnotation().setAnnotations(List.of(badExportAnnotationInstance2));
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
        .setImageUrl("https://dummy.com")
        .setAnnotations(List.of(exportAreaPictureAnnotationInstance()));
  }

  static ExportAreaPictureAnnotationInstance exportAreaPictureAnnotationInstance() {
    return new ExportAreaPictureAnnotationInstance()
        .setLabelName("Polygon A")
        .setStrokeColor("#000000")
        .setFillColor("#00000000")
        .setInfos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo()
                    .setLabel("Type")
                    .setValue("Non renseigné"),
                new ExportAreaPictureAnnotationInstanceInfo()
                    .setLabel("Surface")
                    .setValue("305 m²")))
        .setMeasurements(
            List.of(
                new ExportAreaPictureAnnotationMeasurement()
                    .setValue(20d)
                    .setIsInvisible(false)
                    .setUnit("m"),
                new ExportAreaPictureAnnotationMeasurement()
                    .setValue(20d)
                    .setIsInvisible(true)
                    .setUnit("m"),
                new ExportAreaPictureAnnotationMeasurement()
                    .setValue(20d)
                    .setIsInvisible(true)
                    .setUnit("m")))
        .setPolygon(
            new Polygon()
                .setPoints(
                    List.of(
                        new Point().setX(122d).setY(81d),
                        new Point().setX(184d).setY(176d),
                        new Point().setX(88d).setY(135d),
                        new Point().setX(122d).setY(81d))));
  }
}
