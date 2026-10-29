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
import app.bpartners.api.service.annotation.factory.CustomPageMapper;
import app.bpartners.api.service.annotation.factory.TestContextFactories;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation3D;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotationMeasurement;
import app.bpartners.api.service.annotation.model.export.Point;
import app.bpartners.api.service.annotation.model.export.Polygon;
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

  private static AnnotationReportPdfRenderer pdfGenerator;
  private static AnnotationReportService subject;
  private static BufferedImage mockImage;
  private static byte[] mockImageBytes;

  @BeforeAll
  static void setup() throws IOException {
    mockImage = ImageIO.read(new ClassPathResource("files/rue_de_la_vau.png").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    when(fileService.findById(any())).thenReturn(imageFileInfo());

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
        new CustomPageMapper(fileService)
            .map(
                List.of(
                    // PAGE 1: TEXT SECTIONS (Increasing text length layout showcase)
                    new CustomPage()
                        .pageTitle("Showcase - Sections Textuelles Complètes")
                        .sections(
                            List.of(
                                new TextSection()
                                    .text(
                                        "L'accès au toit par le côté Ouest est limité par la"
                                            + " présence de lignes électriques haute tension."
                                            + " Prudence recommandée.")
                                    .priority(PageSection.PriorityEnum.SMALL)
                                    .type(PageSection.TypeEnum.TEXT),
                                new TextSection()
                                    .text(
                                        "L'accès au toit par le côté Ouest est limité par la"
                                            + " présence de lignes électriques haute tension."
                                            + " Prudence recommandée. Lors de l'inspection"
                                            + " préliminaire des abords du bâtiment, il a été mis"
                                            + " en évidence que les conducteurs aériens surplombent"
                                            + " directement la zone de levage potentielle.")
                                    .priority(PageSection.PriorityEnum.MEDIUM)
                                    .type(PageSection.TypeEnum.TEXT),
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
                                    .caption(
                                        "Rendu pleine largeur des lignes électriques haute tension")
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
                                                List.of(
                                                    "Élément", "Zone affectée", "Niveau de risque"))
                                            .rows(
                                                List.of(
                                                    List.of("Lignes HT", "Flanc Ouest", "Élevé"),
                                                    List.of(
                                                        "Échafaudage", "Pignon Est", "Faible"))))
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
                                                        "Installation barriérage rigide haute"
                                                            + " visibilité"),
                                                    List.of(
                                                        "Avis Enedis",
                                                        "Conducteur Travaux",
                                                        "Demande DICT et isolation provisoire"
                                                            + " câbles"))))
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
                                                    + " ÉLECTRIQUES. L'accès au toit par le côté"
                                                    + " Ouest est limité par la présence de lignes"
                                                    + " électriques haute tension. Prudence"
                                                    + " recommandée. Afin de garantir la sécurité"
                                                    + " absolue des équipes techniques et d'éviter"
                                                    + " tout risque d'amorçage ou d'arc électrique,"
                                                    + " l'utilisation d'échelles métalliques est"
                                                    + " formellement proscrite sur ce flanc."
                                                    + " L'approvisionnement du chantier en"
                                                    + " matériaux ainsi que le montage des"
                                                    + " échafaudages devront être intégralement"
                                                    + " déportés sur le versant Est, qui offre un"
                                                    + " dégagement total et sécurisé.")
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
                                                    + " ÉLECTRIQUES. L'accès au toit par le côté"
                                                    + " Ouest est limité par la présence de lignes"
                                                    + " électriques haute tension. Prudence"
                                                    + " recommandée. Afin de garantir la sécurité"
                                                    + " absolue des équipes techniques et d'éviter"
                                                    + " tout risque d'amorçage ou d'arc électrique,"
                                                    + " l'utilisation d'échelles métalliques est"
                                                    + " formellement proscrite sur ce flanc."
                                                    + " L'approvisionnement du chantier en"
                                                    + " matériaux ainsi que le montage des"
                                                    + " échafaudages devront être intégralement"
                                                    + " déportés sur le versant Est, qui offre un"
                                                    + " dégagement total et sécurisé.")
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
                                    .type(PageSection.TypeEnum.THREE_SPLIT_SECTION))))));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-with-custom-pages");
  }

  @Test
  void generate_from_heavy_payload_without_anything() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = heavyAnnotationFromPayload();
    exportAreaPictureAnnotation.setConf(
        areaPictureAnnotationConfRestMapper.toDomain(
            new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
                .showTitlePage(false)
                .showAnnotationPages(false)
                .showAnnotation3dPages(false)
                .showMeasurementSummary(false)
                .showPitchSummary(false)
                .showAreaSummary(false)
                .showOverallSummary(false)
                .showLlmSummary(false)));

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload-no-title");
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
  void generate_from_angelina_faity_payload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ExportAreaPictureAnnotation exportAreaPictureAnnotation =
        TestContextFactories.readPayload(
            objectMapper,
            new ClassPathResource("payload/export/63 rue angelina faity.json").getInputStream());
    mockImage =
        ImageIO.read(new ClassPathResource("files/63 rue angelina faity.jpeg").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "mysterious-payload");
  }

  @Test
  void generate_from_mysterious_payload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ExportAreaPictureAnnotation exportAreaPictureAnnotation =
        TestContextFactories.readPayload(
            objectMapper, new ClassPathResource("payload/export/payload.json").getInputStream());
    mockImage = ImageIO.read(new ClassPathResource("files/rue_de_la_vau.png").getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "mysterious-payload");
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
  void generate_from_heavy_payload_toulouse() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = annotationFromToulousePayload();
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/17 Rue Pierre Bénech, 31100 Toulouse.png")
                .getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "heavy-payload");
  }

  @Test
  void generate_toulouse_cover_page_without_degradation_note() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = annotationFromToulousePayload();
    exportAreaPictureAnnotation.setConf(
        areaPictureAnnotationConfRestMapper.toDomain(
            new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
                .showTitlePage(true)
                .showAnnotationPages(false)
                .showAnnotation3dPages(false)
                .showMeasurementSummary(false)
                .showPitchSummary(false)
                .showAreaSummary(false)
                .showOverallSummary(false)
                .showLlmSummary(false)
                .showDegradationSummary(false)));
    mockImage =
        ImageIO.read(
            new ClassPathResource("files/17 Rue Pierre Bénech, 31100 Toulouse.png")
                .getInputStream());
    mockImageBytes = toByteStream(mockImage);

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "toulouse-cover-only-no-degradation");
  }

  @Test
  void generate_conches_payload() throws IOException {
    ExportAreaPictureAnnotation exportAreaPictureAnnotation = annotationFromConchesPayload();

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "conches-payload");
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

  @Test
  void generate_shared_ridge_pans_pdf() throws IOException {
    // Two pans sharing a ridge (faitage): check "Résumé des mesures" counts it once, not twice.
    ExportAreaPictureAnnotation exportAreaPictureAnnotation =
        sharedRidgeExportAreaPictureAnnotation();

    byte[] pdfBytes =
        assertDoesNotThrow(
            () -> subject.process(user(), exportAreaPictureAnnotation, mockImage, mockImageBytes));

    assertNotNull(pdfBytes);
    savePdfFile(pdfBytes, "shared-ridge");
  }

  private static void savePdfFile(byte[] pdfBytes, String suffix) throws IOException {
    String now = now().format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "-");
    Files.write(
        Paths.get(String.format("build/annotation-export-%s-%s.pdf", now, suffix)), pdfBytes);
  }

  private ExportAreaPictureAnnotation annotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return TestContextFactories.readPayload(
        objectMapper, new ClassPathResource("payload/export-pdf-payload.json").getInputStream());
  }

  private ExportAreaPictureAnnotation annotationFromConchesPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return TestContextFactories.readPayload(
        objectMapper,
        new ClassPathResource("payload/export/Chem. de Conches 44, 1321 Conches, Suisse.json")
            .getInputStream());
  }

  private ExportAreaPictureAnnotation annotationFromToulousePayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return TestContextFactories.readPayload(
        objectMapper,
        new ClassPathResource("payload/export/17 Rue Pierre Bénech, 31100 Toulouse.json")
            .getInputStream());
  }

  private ExportAreaPictureAnnotation heavyAnnotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var annotation =
        TestContextFactories.readPayload(
            objectMapper,
            new ClassPathResource("payload/heavy-export-pdf-payload.json").getInputStream());

    annotation
        .getThreeD()
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
                          .setLabel("edgeTypes")
                          .setValue(jsonEdgeTypes));
              pan.setImageUri("imageUri");
            });

    annotation.getThreeD().setFacades(new java.util.ArrayList<>(annotation.getThreeD().getPans()));

    return annotation;
  }

  private ExportAreaPictureAnnotation unevenExportAreaPictureAnnotation() {
    return new ExportAreaPictureAnnotation()
        .setImageUrl("https://dummy.com")
        .setAddress("Uneven Annotation Test")
        .setAnnotations(
            List.of(
                exportInstance("Group A", "Type 1", "Good", "10m", 0, 0, 100, 100),
                exportInstance("Group A", "Type 1", "Good", "10m", 100, 0, 200, 100),
                exportInstance("Group B", "Type 2", "Bad", "5m", 0, 100, 100, 200),
                exportInstance("Group C", "Type 3", "Moyen", "2m", 100, 100, 200, 200),
                exportInstance("Group C", "Type 3", "Moyen", "2m", 200, 100, 300, 200),
                exportInstance("Group C", "Type 3", "Moyen", "2m", 300, 100, 400, 200),
                exportInstance("Group D", "Type 4", "N/A", "0m", 0, 200, 100, 300)));
  }

  private ExportAreaPictureAnnotation sharedRidgeExportAreaPictureAnnotation() {
    // Pan1 and Pan2 are two triangular roof faces sharing a ridge between (2000,0) and
    // (1000,1500). Pan2 stores that same edge in reverse point order, as each pan is
    // triangulated independently.
    var pan1 =
        new ExportAreaPictureAnnotation3DPan()
            .setName("P1")
            .setPolygon(
                new Polygon()
                    .setPoints(
                        List.of(
                            new Point().setX(0d).setY(0d),
                            new Point().setX(2000d).setY(0d),
                            new Point().setX(1000d).setY(1500d),
                            new Point().setX(0d).setY(0d))))
            .setMeasurements(
                List.of(
                    new ExportAreaPictureAnnotationMeasurement()
                        .setValue(20.0)
                        .setUnit("m")
                        .setIsInvisible(false),
                    new ExportAreaPictureAnnotationMeasurement()
                        .setValue(18.03)
                        .setUnit("m")
                        .setIsInvisible(false),
                    new ExportAreaPictureAnnotationMeasurement()
                        .setValue(18.03)
                        .setUnit("m")
                        .setIsInvisible(false)))
            .setInfos(
                List.of(
                    new ExportAreaPictureAnnotationInstanceInfo()
                        .setLabel("edgeTypes")
                        .setValue("[\"egout\", \"faitage\", \"aretier\"]"),
                    new ExportAreaPictureAnnotationInstanceInfo().setLabel("Pente").setValue("21°"),
                    new ExportAreaPictureAnnotationInstanceInfo()
                        .setLabel("Surface rampant")
                        .setValue("15m²")));

    var pan2 =
        new ExportAreaPictureAnnotation3DPan()
            .setName("P2")
            .setPolygon(
                new Polygon()
                    .setPoints(
                        List.of(
                            new Point().setX(2000d).setY(0d),
                            new Point().setX(4000d).setY(0d),
                            new Point().setX(1000d).setY(1500d),
                            new Point().setX(2000d).setY(0d))))
            .setMeasurements(
                List.of(
                    new ExportAreaPictureAnnotationMeasurement()
                        .setValue(20.0)
                        .setUnit("m")
                        .setIsInvisible(false),
                    new ExportAreaPictureAnnotationMeasurement()
                        .setValue(22.20)
                        .setUnit("m")
                        .setIsInvisible(false),
                    new ExportAreaPictureAnnotationMeasurement()
                        .setValue(18.03)
                        .setUnit("m")
                        .setIsInvisible(false)))
            .setInfos(
                List.of(
                    new ExportAreaPictureAnnotationInstanceInfo()
                        .setLabel("edgeTypes")
                        .setValue("[\"egout\", \"aretier\", \"faitage\"]"),
                    new ExportAreaPictureAnnotationInstanceInfo().setLabel("Pente").setValue("21°"),
                    new ExportAreaPictureAnnotationInstanceInfo()
                        .setLabel("Surface rampant")
                        .setValue("14m²")));

    return new ExportAreaPictureAnnotation()
        .setImageUrl("https://dummy.com")
        .setAddress("Shared Ridge Test")
        .setThreeD(new ExportAreaPictureAnnotation3D().setPans(List.of(pan1, pan2)));
  }

  private ExportAreaPictureAnnotation fullExportAreaPictureAnnotation() {
    return new ExportAreaPictureAnnotation()
        .setImageUrl("https://dummy.com")
        .setAddress("123 Rue de la Test, 75000 Paris")
        .setGlobalRateValue(75.5)
        .setGlobalRateType("C")
        .setLlm(
            "<h2>Analyse LLM</h2><p>L'état général du bâtiment est satisfaisant. "
                + "Cependant, quelques points d'attention ont été relevés :</p>"
                + "<ul>"
                + "<li>Présence de fissures sur la façade nord 🛠️</li>"
                + "<li>Traces d'humidité près de la gouttière 📸</li>"
                + "<li>Besoin d'un nettoyage approfondi de la toiture 🔍</li>"
                + "</ul>")
        .setAnnotations(
            List.of(
                exportInstance("Top Left", "Corner", "Target", "0,0", 0, 0, 100, 100),
                exportInstance("Top Right", "Corner", "Target", "4096,0", 3996, 0, 4096, 100),
                exportInstance("Bottom Left", "Corner", "Target", "0,5120", 0, 5020, 100, 5120),
                exportInstance(
                    "Bottom Right", "Corner", "Target", "4096,5120", 3996, 5020, 4096, 5120)))
        .setThreeD(
            new ExportAreaPictureAnnotation3D()
                .setPans(
                    List.of(
                        export3DPan("Top Left Pan", "25m²", "Target", 0, 0, 100, 100),
                        export3DPan("Top Right Pan", "22m²", "Target", 3996, 0, 4096, 100),
                        export3DPan("Bottom Left Pan", "30m²", "Target", 0, 5020, 100, 5120),
                        export3DPan("Bottom Right Pan", "28m²", "Target", 3996, 5020, 4096, 5120)))
                .setFacades(
                    List.of(
                        export3DPan("Facade 1", "45m²", "Target", 0, 0, 100, 100),
                        export3DPan("Facade 2", "40m²", "Target", 3996, 0, 4096, 100))));
  }

  private ExportAreaPictureAnnotationInstance exportInstance(
      String key, String type, String etat, String mesure, int x1, int y1, int x2, int y2) {
    return new ExportAreaPictureAnnotationInstance()
        .setFillColor("#FF521B80") // with opacity
        .setStrokeColor("#FF521B")
        .setLabelName(key)
        .setPolygon(dummyPolygon(x1, y1, x2, y2))
        .setMeasurements(
            List.of(
                new ExportAreaPictureAnnotationMeasurement()
                    .setValue(2.0)
                    .setUnit("m")
                    .setIsInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .setValue(2.0)
                    .setUnit("m")
                    .setIsInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .setValue(2.0)
                    .setUnit("m")
                    .setIsInvisible(false),
                new ExportAreaPictureAnnotationMeasurement()
                    .setValue(2.0)
                    .setUnit("m")
                    .setIsInvisible(false)))
        .setInfos(
            List.of(
                new ExportAreaPictureAnnotationInstanceInfo().setLabel("key").setValue(key),
                new ExportAreaPictureAnnotationInstanceInfo().setLabel("Type").setValue(type),
                new ExportAreaPictureAnnotationInstanceInfo().setLabel("État").setValue(etat),
                new ExportAreaPictureAnnotationInstanceInfo().setLabel("Mesure").setValue(mesure)));
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
