package app.bpartners.api.unit.service;

import static app.bpartners.api.file.FileWriter.base64Image;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExportAreaPictureAnnotationPdfGeneratorTest {
  ExportAreaPictureAnnotationPDFGenerator subject =
      new ExportAreaPictureAnnotationPDFGenerator(new TemplateResolverEngine());
  private static BufferedImage mockImage;

  @BeforeAll
  static void createMockImage() throws IOException {
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
  }

  @Test
  void generate_pdf_ok() throws IOException {
    var annotationImage = bufferedImageToBase64(mockImage);
    var exportAreaPictureAnnotation = exportAreaPictureAnnotation();
    var subImages =
        exportAreaPictureAnnotation.getAnnotations().stream()
            .map(annotation -> annotationImage)
            .toList();

    var file =
        assertDoesNotThrow(
            () -> subject.apply(annotationImage, subImages, exportAreaPictureAnnotation));
    assertNotNull(file);
  }

  String bufferedImageToBase64(BufferedImage image) throws IOException {
    var outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);
    return base64Image(outputStream.toByteArray());
  }

  static ExportAreaPictureAnnotation exportAreaPictureAnnotation() {
    return new ExportAreaPictureAnnotation()
        .imageUrl("https://dummy.com")
        .address("Dummy Address")
        .annotations(
            List.of(
                exportAreaPictureAnnotationInstance("key1"),
                exportAreaPictureAnnotationInstance("key1"),
                exportAreaPictureAnnotationInstance("key2"),
                exportAreaPictureAnnotationInstance("key2")));
  }

  static ExportAreaPictureAnnotationInstance exportAreaPictureAnnotationInstance(String key) {
    return new ExportAreaPictureAnnotationInstance()
        .infos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo().label("key").value(key),
                new ExportAreaPictureAnnotationInstanceInfo().label("Type").value("Non renseigné"),
                new ExportAreaPictureAnnotationInstanceInfo().label("Surface").value("305 m²")));
  }
}
