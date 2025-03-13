package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.ClassPathResource;

class ExportAreaPictureAnnotationPdfProcessorTest {
  byte[] fileMock = new byte[] {1, 2, 3, 4};
  ExportAreaPictureAnnotationImageGenerator exportAreaPictureAnnotationImageGeneratorMock = mock();
  ExportAreaPictureAnnotationPDFGenerator exportAreaPictureAnnotationPDFGenerator = mock();
  ExportAreaPictureAnnotation exportAreaPictureAnnotationMock = mock();
  private static BufferedImage mockImage;
  MockedStatic<ImageIO> mockedImageIo = mockStatic(ImageIO.class);
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
    when(exportAreaPictureAnnotationImageGeneratorMock.apply(any(), any(), any()))
        .thenReturn(mockImage);
    when(exportAreaPictureAnnotationPDFGenerator.apply(any(), any(), any())).thenReturn(fileMock);
    when(exportAreaPictureAnnotationMock.getImageUrl()).thenReturn("https://dummy.com");
    mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenReturn(mockImage);
  }

  @AfterEach
  void cleanup() {
    mockedImageIo.close();
  }

  @Test
  void process_pdf_ok() throws IOException {
    var expected = fileMock;

    var actual = subject.process(exportAreaPictureAnnotationMock);

    assertEquals(expected, actual);
  }

  @Test
  void should_throw_if_cannot_read_the_image() {
    mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenThrow(new IOException());

    var error =
        assertThrows(
            BadRequestException.class, () -> subject.process(exportAreaPictureAnnotationMock));

    assertEquals("Cannot read the image from the url", error.getMessage());
  }
}
