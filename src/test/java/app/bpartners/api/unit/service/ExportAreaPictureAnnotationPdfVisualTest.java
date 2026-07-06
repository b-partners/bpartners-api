package app.bpartners.api.unit.service;

import static app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactoryTest.dummyPolygon;
import static app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactoryTest.export3DPan;
import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.*;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

//@Disabled("This is a visual test to generate a PDF file for manual inspection.")
class ExportAreaPictureAnnotationPdfVisualTest {
  private static final ExportAreaPictureAnnotationImageGenerator imageGenerator =
      new ExportAreaPictureAnnotationImageGenerator();
  private static final ExportAreaPictureAnnotationImage3DGenerator image3DGenerator =
      new ExportAreaPictureAnnotationImage3DGenerator();
  private static final AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper =
      new AreaPictureAnnotationConfRestMapper();
  private static final ImageCompressor imageCompressor = new ImageCompressor();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static FileService fileService = mock();

  private static ExportAreaPictureAnnotationPDFGenerator pdfGenerator;
  private static ExportAreaPictureAnnotationPDFProcessor subject;
  private static BufferedImage mockImage;
  private static byte[] mockImageBytes;

  @BeforeAll
  static void setup() throws IOException {
    mockImage = ImageIO.read(new ClassPathResource("files/rue_de_la_vau.png").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    when(fileService.findById(imageFileInfo().getId())).thenReturn(imageFileInfo());

    when(fileService.downloadFile(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              String fileId = invocation.getArgument(2); // Gets the second argument

              if (fileId != null && fileId.equals(user().getLogoFileId())) {
                return new ClassPathResource("files/logo_company.jpeg").getFile();
              }

              return new ClassPathResource("files/rue_de_la_vau.png").getFile();
            });

    pdfGenerator =
        new ExportAreaPictureAnnotationPDFGenerator(
            new TemplateResolverEngine(), fileService, areaPictureAnnotationConfRestMapper);

    subject =
        new ExportAreaPictureAnnotationPDFProcessor(
            pdfGenerator, imageGenerator, image3DGenerator, fileService, imageCompressor);
  }

  private static app.bpartners.api.model.FileInfo imageFileInfo() {
    return app.bpartners.api.model.FileInfo.builder()
        .userUploaderId(user().getId())
        .id("fileInfoId")
        .build();
  }

  private static byte[] toByteStream(BufferedImage bufferedImage) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "png", outputStream);
    return outputStream.toByteArray();
  }

  @Test
  void generate_from_heavy_payload_with_custom_pages() throws IOException, URISyntaxException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setCustomPages(
        List.of(
            // PAGE 1: TEXT SECTIONS (Increasing text length layout showcase)
            new CustomPage()
                .pageTitle("Showcase - Sections Textuelles Complètes")
                .sections(
                    List.of(
                        new TextSection()
                            .text(
                                "L'accès au toit par le côté Ouest est limité par la présence de"
                                    + " lignes électriques haute tension. Prudence recommandée.")
                            .priority(PageSection.PriorityEnum.SMALL)
                            .type(PageSection.TypeEnum.TEXT),
                        new TextSection()
                            .text(
                                "L'accès au toit par le côté Ouest est limité par la présence de"
                                    + " lignes électriques haute tension. Prudence recommandée."
                                    + " Lors de l'inspection préliminaire des abords du bâtiment,"
                                    + " il a été mis en évidence que les conducteurs aériens"
                                    + " surplombent directement la zone de levage potentielle.")
                            .priority(PageSection.PriorityEnum.MEDIUM)
                            .type(PageSection.TypeEnum.TEXT),
                        new TextSection()
                            .text(
                                "RAPPORT DE SÉCURITÉ : ACCÈS EN TOITURE ET RISQUES ÉLECTRIQUES."
                                    + " L'accès au toit par le côté Ouest est limité par la"
                                    + " présence de lignes électriques haute tension. Prudence"
                                    + " recommandée. Afin de garantir la sécurité absolue des"
                                    + " équipes techniques et d'éviter tout risque d'amorçage ou"
                                    + " d'arc électrique, l'utilisation d'échelles métalliques est"
                                    + " formellement proscrite sur ce flanc. L'approvisionnement du"
                                    + " chantier en matériaux ainsi que le montage des échafaudages"
                                    + " devront être intégralement déportés sur le versant Est, qui"
                                    + " offre un dégagement total et sécurisé.")
                            .priority(PageSection.PriorityEnum.IMPORTANT)
                            .type(PageSection.TypeEnum.TEXT))),

            // PAGE 2: IMAGE SECTIONS (Small, Medium, Important scales)
            new CustomPage()
                .pageTitle("Showcase - Sections d'Images Contextuelles")
                .sections(
                    List.of(
                        new ImageSection()
                            .url(
                                new URI(
                                    "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=300"))
                            .caption("Aperçu miniature de la zone de stockage")
                            .priority(PageSection.PriorityEnum.SMALL)
                            .type(PageSection.TypeEnum.IMAGE),
                        new ImageSection()
                            .url(
                                new URI(
                                    "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=300"))
                            .caption("Vue moyenne de l'acheminement des structures")
                            .priority(PageSection.PriorityEnum.MEDIUM)
                            .type(PageSection.TypeEnum.IMAGE),
                        new ImageSection()
                            .url(
                                new URI(
                                    "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=300"))
                            .caption("Rendu pleine largeur des lignes électriques haute tension")
                            .priority(PageSection.PriorityEnum.IMPORTANT)
                            .type(PageSection.TypeEnum.IMAGE))),

            // PAGE 3: TABLE SECTIONS (Small, Medium, Important variations)
            new CustomPage()
                .pageTitle("Showcase - Sections Tableaux Structurels")
                .sections(
                    List.of(
                        new TableSection()
                            .tableData(
                                new TableData()
                                    .headers(List.of("Matériau", "Quantité", "Statut"))
                                    .rows(
                                        List.of(
                                            List.of("Tuiles Canal", "500 u", "Livré"),
                                            List.of("Ciment", "10 sacs", "Attente"))))
                            .priority(PageSection.PriorityEnum.SMALL)
                            .type(PageSection.TypeEnum.TABLE),
                        new TableSection()
                            .tableData(
                                new TableData()
                                    .headers(
                                        List.of("Élément", "Zone affectée", "Niveau de risque"))
                                    .rows(
                                        List.of(
                                            List.of("Lignes HT", "Flanc Ouest", "Élevé"),
                                            List.of("Échafaudage", "Pignon Est", "Faible"))))
                            .priority(PageSection.PriorityEnum.MEDIUM)
                            .type(PageSection.TypeEnum.TABLE),
                        new TableSection()
                            .tableData(
                                new TableData()
                                    .headers(
                                        List.of(
                                            "Intervention",
                                            "Responsable",
                                            "Mesure Prioritaire d'Urgence"))
                                    .rows(
                                        List.of(
                                            List.of(
                                                "Balisage Sol",
                                                "Chef de Chantier",
                                                "Installation barriérage rigide haute visibilité"),
                                            List.of(
                                                "Avis Enedis",
                                                "Conducteur Travaux",
                                                "Demande DICT et isolation provisoire câbles"))))
                            .priority(PageSection.PriorityEnum.IMPORTANT)
                            .type(PageSection.TypeEnum.TABLE)))));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-with-custom-pages");
  }

  private List<PageSection> customTextSection() {
    return List.of(
        new TextSection()
            .text("RAPPORT DE SÉCURITÉ : ACCÈS EN TOITURE ET RISQUES ÉLECTRIQUES")
            .priority(PageSection.PriorityEnum.IMPORTANT)
            .type(PageSection.TypeEnum.TEXT),
        new TextSection()
            .text(
                "L'accès au toit par le côté Ouest est limité par la présence de lignes"
                    + " électriques haute tension. Prudence recommandée. Lors de l'inspection"
                    + " préliminaire des abords du bâtiment, il a été mis en évidence que les"
                    + " conducteurs aériens surplombent directement la zone de levage potentielle,"
                    + " restreignant drastiquement les marges de manœuvre pour le déploiement"
                    + " des dispositifs d'accès temporaires.")
            .priority(PageSection.PriorityEnum.MEDIUM)
            .type(PageSection.TypeEnum.TEXT),
        new TextSection()
            .text("1. Mesures de prévention obligatoires et logistique déportée")
            .priority(PageSection.PriorityEnum.IMPORTANT)
            .type(PageSection.TypeEnum.TEXT),
        new TextSection()
            .text(
                "Afin de garantir la sécurité absolue des équipes techniques et d'éviter tout"
                    + " risque d'amorçage ou d'arc électrique, l'utilisation d'échelles métalliques"
                    + " est formellement proscrite sur ce flanc. L'approvisionnement du chantier en"
                    + " matériaux ainsi que le montage des échafaudages devront être intégralement"
                    + " déportés sur le versant Est, qui offre un dégagement total et sécurisé.")
            .priority(PageSection.PriorityEnum.MEDIUM)
            .type(PageSection.TypeEnum.TEXT),
        new TextSection()
            .text("2. Balisage et zone d'exclusion au sol")
            .priority(PageSection.PriorityEnum.IMPORTANT)
            .type(PageSection.TypeEnum.TEXT),
        new TextSection()
            .text(
                "Une zone d'exclusion temporaire doit être matérialisée au sol par un barriérage"
                    + " rigide pour empêcher le stationnement ou le déploiement d'engins de levage"
                    + " sous les lignes électriques. Toute intervention finale sur la rive Ouest"
                    + " devra faire l'objet d'une signalisation visuelle standardisée signalant le"
                    + " danger mortel à proximité immédiate des câbles sous tension.")
            .priority(PageSection.PriorityEnum.MEDIUM)
            .type(PageSection.TypeEnum.TEXT));
  }

  @Test
  void generate_from_heavy_payload_without_title_page() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
            .showTitlePage(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-title");
  }

  @Test
  void generate_from_heavy_payload_without_annotation_pages() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
            .showAnnotationPages(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-annotation");
  }

  @Test
  void generate_from_heavy_payload_without_annotation_3d_pages() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
            .showAnnotation3dPages(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-annotation-3d");
  }

  @Test
  void generate_from_heavy_payload_without_measurement_summary() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
            .showMeasurementSummary(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-measurement-summary");
  }

  @Test
  void generate_from_heavy_payload_without_pitch_summary() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
            .showPitchSummary(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-pitch-summary");
  }

  @Test
  void generate_from_heavy_payload_without_area_summary() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
            .showAreaSummary(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-area-summary");
  }

  @Test
  void generate_from_heavy_payload_without_overall_summary() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
            .showOverallSummary(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-overall-summary");
  }

  @Test
  void generate_from_heavy_payload_without_llm_summary() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
            .showLlmSummary(false));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-llm-summary");
  }

  @Test
  void generate_from_payload() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = annotationFromPayload();
    mockImage = ImageIO.read(new ClassPathResource("files/rue_de_la_vau.png").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "payload");
  }

  @Test
  void generate_from_heavy_payload() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    mockImage = ImageIO.read(new ClassPathResource("files/rue_de_la_vau.png").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload");
  }

  @Test
  void generate_visual_pdf() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = fullExportAreaPictureAnnotation();

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "visual");
  }

  @Test
  void generate_uneven_annotations_pdf() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = unevenExportAreaPictureAnnotation();

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "uneven");
  }

  private static void savePdfFile(byte[] pdfBytes, String suffix) throws IOException {
    String now = now().format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "-");
    Files.write(
        Paths.get(String.format("build/annotation-export-%s-%s.pdf", now, suffix)), pdfBytes);
  }

  private ExportAreaPictureAnnotation annotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper.readValue(
        new ClassPathResource("payload/export-pdf-payload.json").getInputStream(),
        ExportAreaPictureAnnotation.class);
  }

  private ExportAreaPictureAnnotation heavyAnnotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var annotation =
        objectMapper.readValue(
            new ClassPathResource("payload/heavy-export-pdf-payload.json").getInputStream(),
            ExportAreaPictureAnnotation.class);

    annotation
        .get3d()
        .getPans()
        .forEach(
            pan -> {
              int lines = pan.getPolygon().getPoints().size() - 1;
              var edgeTypes = new String[lines];

              var possibleTypes =
                  Arrays.stream(RoofSlopeBoundaryType.values())
                      .map(t -> t.getLabel().toLowerCase())
                      .toList();
              for (int i = 0; i < lines; i++) {
                var randomType =
                    possibleTypes.get(new Random().nextInt(possibleTypes.size())).replace("_", "-");
                edgeTypes[i] = randomType;
              }

              String jsonEdgeTypes = null;
              try {
                jsonEdgeTypes = objectMapper.writeValueAsString(edgeTypes);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
              pan.getInfos()
                  .add(
                      new ExportAreaPictureAnnotationInstanceInfo()
                          .label("edgeTypes")
                          .value(jsonEdgeTypes));
              pan.setImageUri("imageUri");
            });

    return annotation;
  }

  private ExportAreaPictureAnnotation unevenExportAreaPictureAnnotation() {
    return new ExportAreaPictureAnnotation()
        .imageUrl("https://dummy.com")
        .address("Uneven Annotation Test")
        .annotations(
            List.of(
                exportInstance("Group A", "Type 1", "Good", "10m", 0, 0, 100, 100),
                exportInstance("Group A", "Type 1", "Good", "10m", 100, 0, 200, 100),
                exportInstance("Group B", "Type 2", "Bad", "5m", 0, 100, 100, 200),
                exportInstance("Group C", "Type 3", "Moyen", "2m", 100, 100, 200, 200),
                exportInstance("Group C", "Type 3", "Moyen", "2m", 200, 100, 300, 200),
                exportInstance("Group C", "Type 3", "Moyen", "2m", 300, 100, 400, 200),
                exportInstance("Group D", "Type 4", "N/A", "0m", 0, 200, 100, 300)));
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
