package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.file.ExtensionGuesser;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  ExtensionGuesser extensionGuesserMock = mock();
  FileWriter fileWriter = new FileWriter(new ObjectMapper(), extensionGuesserMock);
  ExportAreaPictureAnnotationPDFGenerator subject =
      new ExportAreaPictureAnnotationPDFGenerator(new TemplateResolverEngine(), fileWriter);
  private static BufferedImage mockImage;

  @BeforeAll
  static void createMockImage() throws IOException {
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
  }

  @BeforeEach
  void setup() {
    when(extensionGuesserMock.apply(any())).thenReturn("pdf");
  }

  @Test
  void generate_pdf_ok() throws IOException {
    var annotationImage = bufferedImageToByteArray(mockImage);
    var exportAreaPictureAnnotation = exportAreaPictureAnnotation();

    var file =
        assertDoesNotThrow(() -> subject.apply(annotationImage, exportAreaPictureAnnotation));
    System.out.println(file.getPath());
  }

  byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
    var outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);
    return outputStream.toByteArray();
  }

  static ExportAreaPictureAnnotation exportAreaPictureAnnotation() {
    return new ExportAreaPictureAnnotation()
        .address("Dummy Address")
        .annotations(
            List.of(
                exportAreaPictureAnnotationInstance(),
                exportAreaPictureAnnotationInstance(),
                exportAreaPictureAnnotationInstance(),
                exportAreaPictureAnnotationInstance()));
  }

  static ExportAreaPictureAnnotationInstance exportAreaPictureAnnotationInstance() {
    return new ExportAreaPictureAnnotationInstance()
        .infos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo().label("Type").value("Non renseigné"),
                new ExportAreaPictureAnnotationInstanceInfo().label("Surface").value("305 m²")));
  }
}
