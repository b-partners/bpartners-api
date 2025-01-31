package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExportAreaPictureAnnotationPdfProcessorTest {
  byte[] fileMock = new byte[] {1, 2, 3, 4};
  ExportAreaPictureAnnotationImageGenerator exportAreaPictureAnnotationImageGeneratorMock = mock();
  ExportAreaPictureAnnotationPDFGenerator exportAreaPictureAnnotationPDFGenerator = mock();
  ExportAreaPictureAnnotation exportAreaPictureAnnotationMock = mock();
  private static BufferedImage mockImage;
  ExportAreaPictureAnnotationPDFProcessor subject =
      new ExportAreaPictureAnnotationPDFProcessor(
          exportAreaPictureAnnotationPDFGenerator, exportAreaPictureAnnotationImageGeneratorMock);

  @BeforeAll
  static void createMockImage() throws IOException {
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
  }

  @BeforeEach
  void setup() {
    when(exportAreaPictureAnnotationImageGeneratorMock.apply(any())).thenReturn(mockImage);
    when(exportAreaPictureAnnotationPDFGenerator.apply(any(), any())).thenReturn(fileMock);
  }

  @Test
  void process_pdf_ok() throws IOException {
    var expected = fileMock;

    var actual = subject.process(exportAreaPictureAnnotationMock);

    assertEquals(expected, actual);
  }
}
