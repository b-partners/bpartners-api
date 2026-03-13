package app.bpartners.api.unit.service;

import static app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactoryTest.dummyPolygon;
import static app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactoryTest.export3DPan;
import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.*;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
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

  private static FileService fileService = mock();

  private static ExportAreaPictureAnnotationPDFGenerator pdfGenerator;
  private static ExportAreaPictureAnnotationPDFProcessor subject;
  private static BufferedImage mockImage;
  private static byte[] mockImageBytes;

  @BeforeAll
  static void setup() throws IOException {
    mockImage =
        ImageIO.read(new ClassPathResource("files/image-with-vegetation.jpg").getInputStream());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(mockImage, "png", outputStream);
    mockImageBytes = outputStream.toByteArray();

    when(fileService.downloadFile(any(), any(), any()))
        .thenReturn(new ClassPathResource("files/logo_company.jpeg").getFile());

    pdfGenerator =
        new ExportAreaPictureAnnotationPDFGenerator(new TemplateResolverEngine(), fileService);

    subject =
        new ExportAreaPictureAnnotationPDFProcessor(
            pdfGenerator, imageGenerator, image3DGenerator, fileService, imageCompressor);
  }

  @Test
  void generate_visual_pdf() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = fullExportAreaPictureAnnotation();

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    String now = now().format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "-");
    Files.write(Paths.get(String.format("build/annotation-export-%s.pdf", now)), pdfBytes);
  }

  private ExportAreaPictureAnnotation fullExportAreaPictureAnnotation() {
    return new ExportAreaPictureAnnotation()
        .imageUrl("https://dummy.com")
        .address("123 Rue de la Test, 75000 Paris")
        .globalRateValue(75.5)
        .globalRateType("C")
        .llm(
            "<h2>Analyse LLM</h2><p>L'état général du bâtiment est satisfaisant. "
                + "Cependant, quelques points d'attention ont été relevés :</p>"
                + "<ul>"
                + "<li>Présence de fissures sur la façade nord 🛠️</li>"
                + "<li>Traces d'humidité près de la gouttière 📸</li>"
                + "<li>Besoin d'un nettoyage approfondi de la toiture 🔍</li>"
                + "</ul>")
        .annotations(
            List.of(
                exportInstance("Top Left", "Corner", "Target", "0,0", 0, 0, 100, 100),
                exportInstance("Top Right", "Corner", "Target", "4096,0", 3996, 0, 4096, 100),
                exportInstance("Bottom Left", "Corner", "Target", "0,5120", 0, 5020, 100, 5120),
                exportInstance(
                    "Bottom Right", "Corner", "Target", "4096,5120", 3996, 5020, 4096, 5120)))
        ._3d(
            new ExportAreaPictureAnnotation3D()
                .pans(
                    List.of(
                        export3DPan("Top Left Pan", "25m²", "Target", 0, 0, 100, 100),
                        export3DPan("Top Right Pan", "22m²", "Target", 3996, 0, 4096, 100),
                        export3DPan("Bottom Left Pan", "30m²", "Target", 0, 5020, 100, 5120),
                        export3DPan(
                            "Bottom Right Pan", "28m²", "Target", 3996, 5020, 4096, 5120))));
  }

  private ExportAreaPictureAnnotationInstance exportInstance(
      String key, String type, String etat, String mesure, int x1, int y1, int x2, int y2) {
    return new ExportAreaPictureAnnotationInstance()
        .fillColor("#FF521B80") // with opacity
        .strokeColor("#FF521B")
        .labelName(key)
        .polygon(dummyPolygon(x1, y1, x2, y2))
        .measurements(
            List.of(
                new ExportAreaPictureAnnotationMeasurement()
                    .value(2.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(2.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(2.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(2.0)
                    .unit("m")
                    .isInvisible(false)))
        .infos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo().label("key").value(key),
                new ExportAreaPictureAnnotationInstanceInfo().label("Type").value(type),
                new ExportAreaPictureAnnotationInstanceInfo().label("État").value(etat),
                new ExportAreaPictureAnnotationInstanceInfo().label("Mesure").value(mesure)));
  }

  app.bpartners.api.model.User user() {
    return User.builder()
        .id("userId")
        .firstName("User")
        .lastName("Name")
        .mobilePhoneNumber("0000000000")
        .email("user@mail.com")
        .logoFileId("logoFileId")
        .accountHolders(
            List.of(AccountHolder.builder().website("https://fancywebsite.com").build()))
        .build();
  }
}
