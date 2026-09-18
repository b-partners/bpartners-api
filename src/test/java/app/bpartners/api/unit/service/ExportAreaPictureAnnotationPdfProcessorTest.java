package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.*;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.file.FileService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper =
      new AreaPictureAnnotationConfRestMapper();

  ExportAreaPictureAnnotationPDFProcessor subject =
      new ExportAreaPictureAnnotationPDFProcessor(
          exportAreaPictureAnnotationPDFGenerator,
          exportAreaPictureAnnotationImageGeneratorMock,
          exportAreaPictureAnnotationImage3DGeneratorMock,
          fileServiceMock,
          imageCompressor,
          areaPictureAnnotationConfRestMapper);

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

    when(exportAreaPictureAnnotationPDFGenerator.apply(any(), any(), any(), any(), any()))
        .thenReturn(fileMock);
    when(exportAreaPictureAnnotationMock.getImageUrl())
        .thenReturn(
            new ClassPathResource("files/downloaded-annotation-image.jpeg")
                .getFile()
                .toURI()
                .toString());
    when(fileServiceMock.downloadFile(any(), any(), any()))
        .thenReturn(new ClassPathResource("files/downloaded-annotation-image.jpeg").getFile());
  }

  @Test
  void process_pdf_ok() throws IOException {
    var expected = fileMock;

    var actual = subject.process(user(), exportAreaPictureAnnotationMock);

    assertEquals(expected, actual);
  }

  @Test
  void process_pdf_skips_image_generation_when_all_pages_hidden() throws IOException {
    // A malformed URL: if the visibility gating is ever broken, the download would be attempted
    // and fail loudly here instead of silently reading the real fallback image.
    when(exportAreaPictureAnnotationMock.getImageUrl()).thenReturn("not a url");
    when(exportAreaPictureAnnotationMock.getConf())
        .thenReturn(
            new ExportAreaPictureAnnotationConf()
                .showTitlePage(false)
                .showAnnotationPages(false)
                .showAnnotation3dPages(false));

    subject.process(user(), exportAreaPictureAnnotationMock);

    verify(exportAreaPictureAnnotationPDFGenerator)
        .apply(any(), isNull(), any(), eq(new Pair<>(null, List.of())), isNull());
  }

  @Test
  void should_throw_if_cannot_read_the_image() {
    when(exportAreaPictureAnnotationMock.getImageUrl())
        .thenReturn("file:///no/such/image-does-not-exist.jpg");

    var error =
        assertThrows(
            BadRequestException.class,
            () -> subject.process(user(), exportAreaPictureAnnotationMock));

    assertEquals("Cannot read the image from the url", error.getMessage());
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
