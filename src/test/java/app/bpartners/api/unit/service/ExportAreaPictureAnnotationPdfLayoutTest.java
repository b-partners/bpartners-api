package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Guards against the title page's main roof photo overflowing onto a second page when it has an
 * unusually tall aspect ratio, which used to push the footer/logo section off the first page.
 */
class ExportAreaPictureAnnotationPdfLayoutTest {
  private static final ExportAreaPictureAnnotationImageGenerator imageGenerator =
      new ExportAreaPictureAnnotationImageGenerator();
  private static final ExportAreaPictureAnnotationImage3DGenerator image3DGenerator =
      new ExportAreaPictureAnnotationImage3DGenerator();
  private static final AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper =
      new AreaPictureAnnotationConfRestMapper();
  private static final ImageCompressor imageCompressor = new ImageCompressor();

  private static FileService fileService = mock();
  private static ExportAreaPictureAnnotationPDFProcessor subject;

  @BeforeAll
  static void setup() throws IOException {
    when(fileService.findById(any()))
        .thenReturn(
            app.bpartners.api.model.FileInfo.builder()
                .userUploaderId(user().getId())
                .id("fileInfoId")
                .build());
    when(fileService.downloadFile(any(), any(), any()))
        .thenReturn(new ClassPathResource("files/logo_company.jpeg").getFile());

    var pdfGenerator =
        new ExportAreaPictureAnnotationPDFGenerator(
            new TemplateResolverEngine(), fileService, areaPictureAnnotationConfRestMapper);

    subject =
        new ExportAreaPictureAnnotationPDFProcessor(
            pdfGenerator, imageGenerator, image3DGenerator, fileService, imageCompressor);
  }

  @Test
  void title_page_with_oversized_tall_image_keeps_footer_on_first_page() throws IOException {
    BufferedImage tallImage = solidColorImage(600, 4000);
    byte[] tallImageBytes = toByteStream(tallImage);

    ExportAreaPictureAnnotation annotation =
        new ExportAreaPictureAnnotation()
            .imageUrl("https://dummy.com")
            .address("1 Rue de Test, 75000 Paris")
            .conf(
                new ExportAreaPictureAnnotationConf()
                    .showTitlePage(true)
                    .showAnnotationPages(false)
                    .showAnnotation3dPages(false)
                    .showMeasurementSummary(false)
                    .showPitchSummary(false)
                    .showAreaSummary(false)
                    .showOverallSummary(false)
                    .showLlmSummary(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), annotation, tallImage, tallImageBytes));

    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      assertEquals(
          1,
          document.getNumberOfPages(),
          "title page should not overflow onto a second page when the main image is unusually"
              + " tall");

      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setStartPage(1);
      stripper.setEndPage(1);
      String firstPageText = stripper.getText(document);

      assertTrue(
          firstPageText.contains(user().getEmail()),
          "footer (with the user's contact info) should stay on the first page alongside the"
              + " logo");
    }
  }

  private static BufferedImage solidColorImage(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = image.createGraphics();
    graphics2D.setColor(Color.LIGHT_GRAY);
    graphics2D.fillRect(0, 0, width, height);
    graphics2D.dispose();
    return image;
  }

  private static byte[] toByteStream(BufferedImage bufferedImage) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "png", outputStream);
    return outputStream.toByteArray();
  }

  static User user() {
    return User.builder()
        .id("userId")
        .firstName("User")
        .lastName("Name")
        .mobilePhoneNumber("0000000000")
        .email("user@mail.com")
        .logoFileId("logoFileId")
        .accountHolders(
            List.of(
                AccountHolder.builder()
                    .website("https://fancywebsite.com")
                    .address("Fancy Address")
                    .build()))
        .build();
  }
}
