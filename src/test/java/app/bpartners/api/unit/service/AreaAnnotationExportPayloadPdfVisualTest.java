package app.bpartners.api.unit.service;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.ExportAreaPictureAnnotationRestMapper;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.annotation.export.AreaAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.export.AreaAnnotationImageGenerator;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFProcessor;
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
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

// @Disabled("This is a visual test to generate a PDF file for manual inspection.")
class AreaAnnotationExportPayloadPdfVisualTest {
  private static final AreaAnnotationImageGenerator imageGenerator =
      new AreaAnnotationImageGenerator();
  private static final AreaAnnotationImage3DGenerator image3DGenerator =
      new AreaAnnotationImage3DGenerator();
  private static final ExportAreaPictureAnnotationRestMapper exportMapper =
      new ExportAreaPictureAnnotationRestMapper();
  private static final ImageCompressor imageCompressor = new ImageCompressor();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static FileService fileService = mock();

  private static AreaAnnotationPDFGenerator pdfGenerator;
  private static AreaAnnotationPDFProcessor subject;
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
              String fileId = invocation.getArgument(2);

              if (fileId != null && fileId.equals(user().getLogoFileId())) {
                return new ClassPathResource("files/logo_company.jpeg").getFile();
              }

              return new ClassPathResource("files/rue_de_la_vau.png").getFile();
            });

    pdfGenerator = new AreaAnnotationPDFGenerator(new TemplateResolverEngine(), fileService);

    subject =
        new AreaAnnotationPDFProcessor(
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
    var restAnnotation = createHeavyRestAnnotationWithCustomPages();
    var domainAnnotation = exportMapper.toDomain(restAnnotation);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), domainAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-with-custom-pages");
  }

  private app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation
      createHeavyRestAnnotationWithCustomPages() throws IOException, java.net.URISyntaxException {
    var annotation = heavyRestAnnotationFromPayload();
    annotation.setCustomPages(
        List.of(
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
                            .type(PageSection.TypeEnum.TABLE))),
            new CustomPage()
                .pageTitle("Showcase - Sections Divisees")
                .sections(
                    List.of(
                        new SplitSection()
                            .leftSection(
                                new TextSection()
                                    .text(
                                        "RAPPORT DE SÉCURITÉ : ACCÈS EN TOITURE ET RISQUES"
                                            + " ÉLECTRIQUES. L'accès au toit par le côté Ouest est"
                                            + " limité par la présence de lignes électriques haute"
                                            + " tension. Prudence recommandée. Afin de garantir la"
                                            + " sécurité absolue des équipes techniques et d'éviter"
                                            + " tout risque d'amorçage ou d'arc électrique,"
                                            + " l'utilisation d'échelles métalliques est"
                                            + " formellement proscrite sur ce flanc."
                                            + " L'approvisionnement du chantier en matériaux ainsi"
                                            + " que le montage des échafaudages devront être"
                                            + " intégralement déportés sur le versant Est, qui"
                                            + " offre un dégagement total et sécurisé.")
                                    .priority(PageSection.PriorityEnum.IMPORTANT)
                                    .type(PageSection.TypeEnum.TEXT))
                            .rightSection(
                                new ImageSection()
                                    .url(
                                        new URI(
                                            "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=300"))
                                    .caption("Aperçu miniature de la zone de stockage")
                                    .priority(PageSection.PriorityEnum.IMPORTANT)
                                    .type(PageSection.TypeEnum.IMAGE))
                            .priority(PageSection.PriorityEnum.IMPORTANT)
                            .type(PageSection.TypeEnum.SPLIT_SECTION))),
            new CustomPage()
                .pageTitle("Showcase - Sections Divisees en Trois")
                .sections(
                    List.of(
                        new ThreeSplitSection()
                            .leftSection(
                                new TextSection()
                                    .text(
                                        "RAPPORT DE SÉCURITÉ : ACCÈS EN TOITURE ET RISQUES"
                                            + " ÉLECTRIQUES. L'accès au toit par le côté Ouest est"
                                            + " limité par la présence de lignes électriques haute"
                                            + " tension. Prudence recommandée. Afin de garantir la"
                                            + " sécurité absolue des équipes techniques et d'éviter"
                                            + " tout risque d'amorçage ou d'arc électrique,"
                                            + " l'utilisation d'échelles métalliques est"
                                            + " formellement proscrite sur ce flanc."
                                            + " L'approvisionnement du chantier en matériaux ainsi"
                                            + " que le montage des échafaudages devront être"
                                            + " intégralement déportés sur le versant Est, qui"
                                            + " offre un dégagement total et sécurisé.")
                                    .priority(PageSection.PriorityEnum.IMPORTANT)
                                    .type(PageSection.TypeEnum.TEXT))
                            .middleSection(
                                new TextSection()
                                    .text("Colonne milieu")
                                    .priority(PageSection.PriorityEnum.SMALL)
                                    .type(PageSection.TypeEnum.TEXT))
                            .rightSection(
                                new ImageSection()
                                    .url(
                                        new URI(
                                            "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=300"))
                                    .caption("Aperçu miniature de la zone de stockage")
                                    .priority(PageSection.PriorityEnum.IMPORTANT)
                                    .type(PageSection.TypeEnum.IMAGE))
                            .priority(PageSection.PriorityEnum.MEDIUM)
                            .type(PageSection.TypeEnum.THREE_SPLIT_SECTION)))));
    return annotation;
  }

  @Test
  void generate_from_heavy_payload_without_anything() throws IOException {
    var restAnnotation = heavyRestAnnotationFromPayload();
    restAnnotation.setConf(
        new ExportAreaPictureAnnotationConf()
            .showTitlePage(false)
            .showAnnotationPages(false)
            .showAnnotation3dPages(false)
            .showMeasurementSummary(false)
            .showPitchSummary(false)
            .showAreaSummary(false)
            .showOverallSummary(false)
            .showLlmSummary(false));
    var domainAnnotation = exportMapper.toDomain(restAnnotation);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), domainAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-title");
  }

  @Test
  void generate_from_payload() throws IOException {
    var restAnnotation = restAnnotationFromPayload();
    var domainAnnotation = exportMapper.toDomain(restAnnotation);
    mockImage = ImageIO.read(new ClassPathResource("files/rue_de_la_vau.png").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), domainAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "payload");
  }

  @Test
  void generate_from_heavy_payload() throws IOException {
    var restAnnotation = heavyRestAnnotationFromPayload();
    var domainAnnotation = exportMapper.toDomain(restAnnotation);
    mockImage = ImageIO.read(new ClassPathResource("files/rue_de_la_vau.png").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), domainAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload");
  }

  @Test
  void generate_from_heavy_payload_toulouse() throws IOException {
    var restAnnotation = restAnnotationFromToulousePayload();
    var domainAnnotation = exportMapper.toDomain(restAnnotation);
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/17 Rue Pierre Bénech, 31100 Toulouse.png")
                .getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), domainAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "toulouse-payload");
  }

  @Test
  void generate_visual_pdf() throws IOException {
    var restAnnotation = fullRestExportAreaPictureAnnotation();
    var domainAnnotation = exportMapper.toDomain(restAnnotation);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), domainAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "visual");
  }

  @Test
  void generate_uneven_annotations_pdf() throws IOException {
    var restAnnotation = unevenRestExportAreaPictureAnnotation();
    var domainAnnotation = exportMapper.toDomain(restAnnotation);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), domainAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "uneven");
  }

  private static void savePdfFile(byte[] pdfBytes, String suffix) throws IOException {
    String now = now().format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "-");
    Files.write(
        Paths.get(String.format("build/annotation-export-%s-%s.pdf", now, suffix)), pdfBytes);
  }

  private app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation
      restAnnotationFromToulousePayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper.readValue(
        new ClassPathResource("payload/export/17 Rue Pierre Bénech, 31100 Toulouse.json")
            .getInputStream(),
        app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation.class);
  }

  private app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation
      restAnnotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper.readValue(
        new ClassPathResource("payload/export-pdf-payload.json").getInputStream(),
        app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation.class);
  }

  private app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation
      heavyRestAnnotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var annotation =
        objectMapper.readValue(
            new ClassPathResource("payload/heavy-export-pdf-payload.json").getInputStream(),
            app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation.class);

    if (annotation.get3d() != null) {
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
                      possibleTypes
                          .get(new Random().nextInt(possibleTypes.size()))
                          .replace("_", "-");
                  edgeTypes[i] = randomType;
                }
                String jsonEdgeTypes;
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

      annotation.get3d().setFacades(new java.util.ArrayList<>(annotation.get3d().getPans()));
    }

    return annotation;
  }

  private app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation
      unevenRestExportAreaPictureAnnotation() {
    return new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation()
        .imageUrl("https://dummy.com")
        .address("Uneven Annotation Test")
        .annotations(
            List.of(
                restExportInstance("Group A", "Type 1", "Good", "10m", 0, 0, 100, 100),
                restExportInstance("Group A", "Type 1", "Good", "10m", 100, 0, 200, 100),
                restExportInstance("Group B", "Type 2", "Bad", "5m", 0, 100, 100, 200),
                restExportInstance("Group C", "Type 3", "Moyen", "2m", 100, 100, 200, 200),
                restExportInstance("Group C", "Type 3", "Moyen", "2m", 200, 100, 300, 200),
                restExportInstance("Group C", "Type 3", "Moyen", "2m", 300, 100, 400, 200),
                restExportInstance("Group D", "Type 4", "N/A", "0m", 0, 200, 100, 300)));
  }

  private app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan restExport3DPan(
      String name, String surface, String observation, int x1, int y1, int x2, int y2) {
    return new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan()
        .name(name)
        .polygon(dummyRestPolygon(x1, y1, x2, y2))
        .imageUri(null)
        .measurements(
            java.util.List.of(
                new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement()
                    .value(5.0)
                    .unit("m")
                    .isInvisible(false),
                new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement()
                    .value(5.0)
                    .unit("m")
                    .isInvisible(false),
                new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement()
                    .value(5.0)
                    .unit("m")
                    .isInvisible(false),
                new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement()
                    .value(5.0)
                    .unit("m")
                    .isInvisible(false)))
        .infos(
            java.util.List.of(
                new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo()
                    .label("Surface")
                    .value(surface),
                new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo()
                    .label("Observation")
                    .value(observation)));
  }

  private app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation
      fullRestExportAreaPictureAnnotation() {
    return new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation()
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
                restExportInstance("Top Left", "Corner", "Target", "0,0", 0, 0, 100, 100),
                restExportInstance("Top Right", "Corner", "Target", "4096,0", 3996, 0, 4096, 100),
                restExportInstance("Bottom Left", "Corner", "Target", "0,5120", 0, 5020, 100, 5120),
                restExportInstance(
                    "Bottom Right", "Corner", "Target", "4096,5120", 3996, 5020, 4096, 5120)))
        ._3d(
            new ExportAreaPictureAnnotation3D()
                .pans(
                    List.of(
                        restExport3DPan("Top Left Pan", "25m²", "Target", 0, 0, 100, 100),
                        restExport3DPan("Top Right Pan", "22m²", "Target", 3996, 0, 4096, 100),
                        restExport3DPan("Bottom Left Pan", "30m²", "Target", 0, 5020, 100, 5120),
                        restExport3DPan(
                            "Bottom Right Pan", "28m²", "Target", 3996, 5020, 4096, 5120)))
                .facades(
                    List.of(
                        restExport3DPan("Facade 1", "45m²", "Target", 0, 0, 100, 100),
                        restExport3DPan("Facade 2", "40m²", "Target", 3996, 0, 4096, 100))));
  }

  private app.bpartners.api.endpoint.rest.model.Polygon dummyRestPolygon(
      int x1, int y1, int x2, int y2) {
    return new app.bpartners.api.endpoint.rest.model.Polygon()
        .points(
            java.util.List.of(
                new app.bpartners.api.endpoint.rest.model.Point().x((double) x1).y((double) y1),
                new app.bpartners.api.endpoint.rest.model.Point().x((double) x2).y((double) y1),
                new app.bpartners.api.endpoint.rest.model.Point().x((double) x2).y((double) y2),
                new app.bpartners.api.endpoint.rest.model.Point().x((double) x1).y((double) y2),
                new app.bpartners.api.endpoint.rest.model.Point().x((double) x1).y((double) y1)));
  }

  private app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance
      restExportInstance(
          String key, String type, String etat, String mesure, int x1, int y1, int x2, int y2) {
    return new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance()
        .fillColor("#FF521B80")
        .strokeColor("#FF521B")
        .labelName(key)
        .polygon(dummyRestPolygon(x1, y1, x2, y2))
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
