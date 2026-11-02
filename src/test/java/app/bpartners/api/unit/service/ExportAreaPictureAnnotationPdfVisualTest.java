package app.bpartners.api.unit.service;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.*;
import app.bpartners.api.service.annotation.factory.TestContextFactories;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Disabled("This is a visual test to generate a PDF file for manual inspection.")
class ExportAreaPictureAnnotationPdfVisualTest {
  private static final ExportAreaPictureAnnotationImageGenerator imageGenerator =
      new ExportAreaPictureAnnotationImageGenerator();
  private static final ExportAreaPictureAnnotationImage3DGenerator image3DGenerator =
      new ExportAreaPictureAnnotationImage3DGenerator();
  private static final ImageCompressor imageCompressor = new ImageCompressor();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static FileService fileService = mock();

  private static AnnotationReportPdfRenderer pdfGenerator;
  private static AnnotationReportService subject;
  private static BufferedImage mockImage;
  private static byte[] mockImageBytes;

  @BeforeAll
  static void setup() throws IOException {
    mockImage = ImageIO.read(new ClassPathResource("files/rue_de_la_vau.png").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    when(fileService.findById(any()))
        .thenAnswer(
            invocation ->
                app.bpartners.api.model.FileInfo.builder()
                    .userUploaderId(user().getId())
                    .id(invocation.getArgument(0))
                    .build());

    when(fileService.downloadFile(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              String fileId = invocation.getArgument(2); // Gets the second argument

              if (fileId != null && fileId.equals(user().getLogoFileId())) {
                return new ClassPathResource("files/logo_company.jpeg").getFile();
              }

              // pan/facade images extracted from the reference PDFs, named after their imageUri
              var itemImage = new ClassPathResource("files/items/" + fileId + ".jpg");
              if (itemImage.exists()) {
                return itemImage.getFile();
              }

              return new ClassPathResource("files/rue_de_la_vau.png").getFile();
            });

    pdfGenerator =
        new AnnotationReportPdfRenderer(
            new TemplateResolverEngine(), TestContextFactories.contextFactory(fileService));

    subject =
        new AnnotationReportService(
            pdfGenerator,
            imageGenerator,
            image3DGenerator,
            fileService,
            imageCompressor,
            new app.bpartners.api.service.annotation.ImageDownloader());
  }

  private static byte[] toByteStream(BufferedImage bufferedImage) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "png", outputStream);
    return outputStream.toByteArray();
  }

  @Test
  void generate_parthenay_pdf() throws IOException {
    generatePdf(
        "payload/export/1 Rue de la Vau Saint-Jacques, 79200 Parthenay.json",
        "files/1 Rue de la Vau Saint-Jacques, 79200 Parthenay.jpg",
        "parthenay");
  }

  @Test
  void generate_toulouse_pdf() throws IOException {
    generatePdf(
        "payload/export/17 Rue Pierre Bénech, 31100 Toulouse.json",
        "files/17 Rue Pierre Bénech, 31100 Toulouse.png",
        "toulouse");
  }

  @Test
  void generate_morges_pdf() throws IOException {
    generatePdf(
        "payload/export/Paul Benoit - Chem. des Terrasses 7, 1110 Morges, Suisse.json",
        "files/Paul Benoit - Chem. des Terrasses 7, 1110 Morges, Suisse.png",
        "morges");
  }

  private static void generatePdf(String payloadPath, String imagePath, String suffix)
      throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ExportAreaPictureAnnotation exportAreaPictureAnnotation =
        TestContextFactories.readPayload(
            objectMapper, new ClassPathResource(payloadPath).getInputStream());
    mockImage = ImageIO.read(new ClassPathResource(imagePath).getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, suffix);
  }

  private static void savePdfFile(byte[] pdfBytes, String suffix) throws IOException {
    String now = now().format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "-");
    Files.write(
        Paths.get(String.format("build/annotation-export-%s-%s.pdf", now, suffix)), pdfBytes);
  }

  static app.bpartners.api.model.User user() {
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
