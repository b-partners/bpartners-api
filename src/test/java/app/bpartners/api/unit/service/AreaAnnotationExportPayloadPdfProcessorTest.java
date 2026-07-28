package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.annotation.export.AreaAnnotationExportConf;
import app.bpartners.api.service.annotation.export.AreaAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.export.AreaAnnotationImageGenerator;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFProcessor;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.file.FileService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.ClassPathResource;

class AreaAnnotationExportPayloadPdfProcessorTest {
  byte[] fileMock = new byte[] {1, 2, 3, 4};
  private static BufferedImage mockImage;

  AreaAnnotationExportPayload areaAnnotationExportPayload =
      AreaAnnotationExportPayload.builder()
          .imageUrl("https://dummy.com")
          .address("Test Address")
          .conf(AreaAnnotationExportConf.DEFAULT)
          .build();
  AreaAnnotationPDFGenerator areaAnnotationPDFGenerator = mock();
  AreaAnnotationImageGenerator areaAnnotationImageGeneratorMock = mock();
  AreaAnnotationImage3DGenerator areaAnnotationImage3DGeneratorMock = mock();
  FileService fileServiceMock = mock();
  ImageCompressor imageCompressor = new ImageCompressor();

  AreaAnnotationPDFProcessor subject =
      new AreaAnnotationPDFProcessor(
          areaAnnotationPDFGenerator,
          areaAnnotationImageGeneratorMock,
          areaAnnotationImage3DGeneratorMock,
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
    when(areaAnnotationImageGeneratorMock.apply(any(), any(), any())).thenReturn(mockImage);
    when(areaAnnotationImage3DGeneratorMock.generateBaseImage(any()))
        .thenReturn(new Pair<>(mock(), mockImage));

    when(areaAnnotationPDFGenerator.apply(any(), any(), any(), any(), any(), any()))
        .thenReturn(fileMock);
    when(fileServiceMock.downloadFile(any(), any(), any()))
        .thenReturn(new ClassPathResource("files/downloaded-annotation-image.jpeg").getFile());
  }

  @Test
  void process_pdf_ok() throws IOException {
    try (MockedStatic<ImageIO> mockedImageIo = mockStatic(ImageIO.class)) {
      mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenReturn(mockImage);
      var expected = fileMock;

      var actual = subject.process(user(), areaAnnotationExportPayload);

      assertEquals(expected, actual);
    }
  }

  @Test
  void should_throw_if_cannot_read_the_image() {
    try (MockedStatic<ImageIO> mockedImageIo = mockStatic(ImageIO.class)) {
      mockedImageIo.when(() -> ImageIO.read(any(URL.class))).thenThrow(new IOException());

      var error =
          assertThrows(
              BadRequestException.class,
              () -> subject.process(user(), areaAnnotationExportPayload));

      assertEquals("Cannot read the image from the url", error.getMessage());
    }
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
