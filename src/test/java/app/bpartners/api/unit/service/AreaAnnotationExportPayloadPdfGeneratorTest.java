package app.bpartners.api.unit.service;

import static app.bpartners.api.file.FileWriter.base64Image;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.AreaAnnotationInstance;
import app.bpartners.api.service.annotation.AreaAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.export.AreaAnnotationExportConf;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFGenerator;
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

class AreaAnnotationExportPayloadPdfGeneratorTest {
  FileService fileService = mock();
  AreaAnnotationPDFGenerator subject;

  private static BufferedImage mockImage;

  @BeforeAll
  static void createMockImage() throws IOException {
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
  }

  @BeforeEach
  void setUp() {
    when(fileService.downloadFile(any(), any(), any())).thenReturn(null);

    subject = new AreaAnnotationPDFGenerator(new TemplateResolverEngine(), fileService);
  }

  @Test
  void generate_pdf_ok() throws IOException {
    var annotationImage = bufferedImageToBase64(mockImage);
    var logoImage = bufferedImageToBase64(mockImage);
    var exportAreaPictureAnnotation = exportDomainAnnotation();
    var subImages =
        exportAreaPictureAnnotation.getAnnotations().stream()
            .map(annotation -> annotationImage)
            .toList();

    var file =
        assertDoesNotThrow(
            () ->
                subject.apply(
                    user(),
                    logoImage,
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

  static AreaAnnotationExportPayload exportDomainAnnotation() {
    return AreaAnnotationExportPayload.builder()
        .imageUrl("https://dummy.com")
        .address("Dummy Address")
        .conf(AreaAnnotationExportConf.DEFAULT)
        .annotations(
            List.of(
                exportDomainInstance("key1"),
                exportDomainInstance("key1"),
                exportDomainInstance("key2"),
                exportDomainInstance("key2")))
        .build();
  }

  static AreaAnnotationInstance exportDomainInstance(String key) {
    return AreaAnnotationInstance.builder()
        .infos(
            List.of(
                new AreaAnnotationInstanceInfo("key", key),
                new AreaAnnotationInstanceInfo("Type", "Non renseigné"),
                new AreaAnnotationInstanceInfo("Surface", "305 m²")))
        .build();
  }
}
