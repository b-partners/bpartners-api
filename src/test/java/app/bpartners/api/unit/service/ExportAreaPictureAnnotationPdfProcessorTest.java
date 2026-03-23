package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.*;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.file.FileService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.ClassPathResource;

class ExportAreaPictureAnnotationPdfProcessorTest {
  byte[] fileMock = new byte[] {1, 2, 3, 4};
  private static BufferedImage mockImage;

  ExportAreaPictureAnnotation exportAreaPictureAnnotationMock = mock();
  ExportAreaPictureAnnotationPDFGenerator exportAreaPictureAnnotationPDFGenerator = mock();
  ExportAreaPictureAnnotationImageGenerator exportAreaPictureAnnotationImageGeneratorMock = mock();
  ExportAreaPictureAnnotationImage3DGenerator exportAreaPictureAnnotationImage3DGeneratorMock =
      mock();
  FileService fileServiceMock = mock();
  ImageCompressor imageCompressor = new ImageCompressor();

  ExportAreaPictureAnnotationPDFProcessor subject =
      new ExportAreaPictureAnnotationPDFProcessor(
          exportAreaPictureAnnotationPDFGenerator,
          exportAreaPictureAnnotationImageGeneratorMock,
          exportAreaPictureAnnotationImage3DGeneratorMock,
          fileServiceMock,
          imageCompressor);

  @BeforeAll
  static void createMockImage() throws IOException {
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
  }

  @BeforeEach
  void setup() throws IOException {
    when(exportAreaPictureAnnotationImageGeneratorMock.apply(any(), any(), any()))
        .thenReturn(mockImage);
    when(exportAreaPictureAnnotationImage3DGeneratorMock.generateBaseImage(any()))
        .thenReturn(new Pair<>(mock(), mockImage));
    when(exportAreaPictureAnnotationImage3DGeneratorMock.generatePanImage(any(), any(), any()))
        .thenReturn(mockImage);

    when(exportAreaPictureAnnotationPDFGenerator.apply(any(), any(), any(), any(), any()))
        .thenReturn(fileMock);
    when(exportAreaPictureAnnotationMock.getImageUrl()).thenReturn("https://dummy.com");
    when(fileServiceMock.downloadFile(any(), any(), any()))
        .thenReturn(new ClassPathResource("files/downloaded-annotation-image.jpeg").getFile());
  }

  @Test
  @Disabled("TODO: Exception: cannot read the image from the url")
  void process_pdf_ok() throws IOException {
    MockedStatic<ImageIO> mockedImageIo = mockStatic(ImageIO.class);
    mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenReturn(mockImage);
    subject =
        new ExportAreaPictureAnnotationPDFProcessor(
            exportAreaPictureAnnotationPDFGenerator,
            exportAreaPictureAnnotationImageGeneratorMock,
            exportAreaPictureAnnotationImage3DGeneratorMock,
            fileServiceMock,
            imageCompressor);
    var expected = fileMock;

    var actual = subject.process(user(), exportAreaPictureAnnotationMock);

    assertEquals(expected, actual);
    mockedImageIo.close();
  }

  @Test
  void should_throw_if_cannot_read_the_image() {
    MockedStatic<ImageIO> mockedImageIo = mockStatic(ImageIO.class);
    mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenThrow(new IOException());

    var error =
        assertThrows(
            BadRequestException.class,
            () -> subject.process(user(), exportAreaPictureAnnotationMock));

    assertEquals("Cannot read the image from the url", error.getMessage());
    mockedImageIo.close();
  }

  User user() {
    return User.builder()
        .id("userId")
        .firstName("User")
        .lastName("Name")
        .mobilePhoneNumber("0000000000")
        .email("user@mail.com")
        .build();
  }
}
