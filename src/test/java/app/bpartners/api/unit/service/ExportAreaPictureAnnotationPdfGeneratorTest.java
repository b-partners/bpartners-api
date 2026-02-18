package app.bpartners.api.unit.service;

import static app.bpartners.api.file.FileWriter.base64Image;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExportAreaPictureAnnotationPdfGeneratorTest {
  FileService fileServiceMock = mock();
  ExportAreaPictureAnnotationPDFGenerator subject;
  private static BufferedImage mockImage;

  @BeforeAll
  static void createMockImage() throws IOException {
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
  }

  @BeforeEach
  void setUp() {
    when(fileServiceMock.downloadFile(any(), any(), any())).thenReturn(null);
    subject =
        new ExportAreaPictureAnnotationPDFGenerator(new TemplateResolverEngine(), fileServiceMock);
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
            () ->
                subject.apply(
                    user(),
                    exportAreaPictureAnnotation,
                    new Pair<>(annotationImage, subImages),
                    null));
    assertNotNull(file);
  }

  String bufferedImageToBase64(BufferedImage image) throws IOException {
    var outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);
    return base64Image(outputStream.toByteArray());
  }

  User user() {
    return User.builder()
        .firstName("User")
        .lastName("Name")
        .mobilePhoneNumber("0000000000")
        .email("user@mail.com")
        .build();
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
