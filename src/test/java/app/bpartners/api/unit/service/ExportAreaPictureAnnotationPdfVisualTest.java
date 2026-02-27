package app.bpartners.api.unit.service;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.*;
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

  private static FileService fileService = mock();

  private static ExportAreaPictureAnnotationPDFGenerator pdfGenerator;
  private static ExportAreaPictureAnnotationPDFProcessor subject;
  private static BufferedImage mockImage;
  private static byte[] mockImageBytes;

  @BeforeAll
  static void setup() throws IOException {
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/downloaded-annotation-image.jpeg").getInputStream());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(mockImage, "png", outputStream);
    mockImageBytes = outputStream.toByteArray();

    when(fileService.downloadFile(any(), any(), any()))
        .thenReturn(new ClassPathResource("files/logo_company.jpeg").getFile());

    pdfGenerator =
        new ExportAreaPictureAnnotationPDFGenerator(new TemplateResolverEngine(), fileService);

    subject =
        new ExportAreaPictureAnnotationPDFProcessor(
            pdfGenerator, imageGenerator, image3DGenerator, fileService);
  }

  @Test
  void generate_visual_pdf() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = fullExportAreaPictureAnnotation();

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    String now = now().format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "-");
    Files.write(
        Paths.get(String.format("build/little-fonted-annotation-export-%s.pdf", now)), pdfBytes);
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
                exportInstance("Façade", "Fissure", "Légère", "2m", 50, 50, 150, 150),
                exportInstance("Façade", "Fissure", "Moyenne", "1.5m", 200, 50, 300, 150),
                exportInstance("Toiture", "Mousse", "Abondante", "50m²", 50, 200, 150, 300),
                exportInstance(
                    "Fenêtre RDC", "Vitrage", "Double", "1.2m x 1.5m", 200, 200, 300, 300),
                exportInstance("Porte Entrée", "Matériau", "Bois", "Neuf", 350, 50, 450, 150)))
        ._3d(
            new ExportAreaPictureAnnotation3D()
                .pans(
                    List.of(
                        export3DPan("Pan Est", "25m²", "Bon état", 50, 50, 150, 150),
                        export3DPan("Pan Ouest", "22m²", "À rénover", 200, 50, 300, 150),
                        export3DPan("Pan Sud", "30m²", "Excellent", 50, 200, 150, 300),
                        export3DPan("Pan Nord", "28m²", "Moyen", 200, 200, 300, 300),
                        export3DPan("Garage", "15m²", "Neuf", 350, 50, 450, 150))));
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

  private ExportAreaPictureAnnotation3DPan export3DPan(
      String name, String surface, String observation, int x1, int y1, int x2, int y2) {
    return new ExportAreaPictureAnnotation3DPan()
        .name(name)
        .polygon(dummyPolygon(x1, y1, x2, y2))
        .measurements(
            List.of(
                new ExportAreaPictureAnnotationMeasurement()
                    .value(5.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(5.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(5.0)
                    .unit("m")
                    .isInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .value(5.0)
                    .unit("m")
                    .isInvisible(false)))
        .infos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo().label("Surface").value(surface),
                new ExportAreaPictureAnnotationInstanceInfo()
                    .label("Observation")
                    .value(observation)));
  }

  app.bpartners.api.model.User user() {
    return User.builder()
        .id("userId")
        .firstName("User")
        .lastName("Name")
        .mobilePhoneNumber("0000000000")
        .email("user@mail.com")
        .logoFileId("logoFileId")
        .build();
  }

  private Polygon dummyPolygon(int x1, int y1, int x2, int y2) {
    return new Polygon()
        .points(
            List.of(
                new Point().x((double) x1).y((double) y1),
                new Point().x((double) x2).y((double) y1),
                new Point().x((double) x2).y((double) y2),
                new Point().x((double) x1).y((double) y2),
                new Point().x((double) x1).y((double) y1) // Close the polygon
                ));
  }
}
