package app.bpartners.api.unit.service;

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
  private static final String PARTHENAY_PAYLOAD =
      "payload/export/1 Rue de la Vau Saint-Jacques, 79200 Parthenay.json";
  private static final String PARTHENAY_IMAGE =
      "files/1 Rue de la Vau Saint-Jacques, 79200 Parthenay.jpg";
  // Parthenay "Façade A" image, resolved through the mocked file service
  private static final String CUSTOM_PAGE_IMAGE_FILE_ID = "d4f83366-3766-48d5-85d1-5e3d96fa65ef";
  private static final String LONG_TEXT =
      "RAPPORT DE SÉCURITÉ : ACCÈS EN TOITURE ET RISQUES ÉLECTRIQUES. L'accès au toit par le côté"
          + " Ouest est limité par la présence de lignes électriques haute tension. Prudence"
          + " recommandée. Afin de garantir la sécurité absolue des équipes techniques et d'éviter"
          + " tout risque d'amorçage ou d'arc électrique, l'utilisation d'échelles métalliques est"
          + " formellement proscrite sur ce flanc. L'approvisionnement du chantier en matériaux"
          + " ainsi que le montage des échafaudages devront être intégralement déportés sur le"
          + " versant Est, qui offre un dégagement total et sécurisé.";
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
    generatePdf(PARTHENAY_PAYLOAD, PARTHENAY_IMAGE, "parthenay");
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

  @Test
  void generate_custom_pages_only_pdf() throws IOException {
    var annotation = readAnnotation(PARTHENAY_PAYLOAD);
    annotation.setConf(
        new AreaPictureAnnotationConfRestMapper()
            .toDomain(
                new app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationConf()
                    .showTitlePage(false)
                    .showAnnotationPages(false)
                    .showAnnotation3dPages(false)
                    .showMeasurementSummary(false)
                    .showPitchSummary(false)
                    .showAreaSummary(false)
                    .showOverallSummary(false)
                    .showLlmSummary(false)
                    .showDegradationSummary(false)));
    annotation.setCustomPages(new CustomPageMapper(fileService).map(allTypesCustomPages()));

    renderPdf(annotation, PARTHENAY_IMAGE, "custom-pages-only");
  }

  private static List<CustomPage> allTypesCustomPages() {
    return List.of(
        new CustomPage()
            .pageTitle("Sections Textuelles")
            .sections(
                List.of(
                    text(PageSection.PriorityEnum.SMALL, "Accès au toit limité côté Ouest."),
                    text(
                        PageSection.PriorityEnum.MEDIUM,
                        "L'accès au toit par le côté Ouest est limité par la présence de lignes"
                            + " électriques haute tension. Prudence recommandée."),
                    text(PageSection.PriorityEnum.IMPORTANT, LONG_TEXT))),
        new CustomPage()
            .pageTitle("Sections d'Images")
            .sections(
                List.of(
                    image(PageSection.PriorityEnum.SMALL, "Miniature de la zone de stockage"),
                    image(PageSection.PriorityEnum.MEDIUM, "Vue moyenne de l'acheminement"),
                    image(PageSection.PriorityEnum.IMPORTANT, "Rendu pleine largeur"))),
        new CustomPage()
            .pageTitle("Sections Tableaux")
            .sections(
                List.of(
                    table(
                        PageSection.PriorityEnum.SMALL,
                        List.of("Matériau", "Quantité", "Statut"),
                        List.of(
                            List.of("Tuiles Canal", "500 u", "Livré"),
                            List.of("Ciment", "10 sacs", "Attente"))),
                    table(
                        PageSection.PriorityEnum.MEDIUM,
                        List.of("Élément", "Zone affectée", "Niveau de risque"),
                        List.of(
                            List.of("Lignes HT", "Flanc Ouest", "Élevé"),
                            List.of("Échafaudage", "Pignon Est", "Faible"))),
                    table(
                        PageSection.PriorityEnum.IMPORTANT,
                        List.of("Intervention", "Responsable", "Mesure Prioritaire d'Urgence"),
                        List.of(
                            List.of(
                                "Balisage Sol",
                                "Chef de Chantier",
                                "Installation barriérage rigide haute visibilité"),
                            List.of(
                                "Avis Enedis",
                                "Conducteur Travaux",
                                "Demande DICT et isolation provisoire câbles"))))),
        new CustomPage()
            .pageTitle("Sections Divisées")
            .sections(
                List.of(
                    new SplitSection()
                        .leftSection(text(PageSection.PriorityEnum.IMPORTANT, LONG_TEXT))
                        .rightSection(image(PageSection.PriorityEnum.IMPORTANT, "Zone de stockage"))
                        .priority(PageSection.PriorityEnum.IMPORTANT)
                        .type(PageSection.TypeEnum.SPLIT_SECTION))),
        new CustomPage()
            .pageTitle("Sections Divisées en Trois")
            .sections(
                List.of(
                    new ThreeSplitSection()
                        .leftSection(text(PageSection.PriorityEnum.IMPORTANT, LONG_TEXT))
                        .middleSection(text(PageSection.PriorityEnum.SMALL, "Colonne milieu"))
                        .rightSection(image(PageSection.PriorityEnum.IMPORTANT, "Zone de stockage"))
                        .priority(PageSection.PriorityEnum.MEDIUM)
                        .type(PageSection.TypeEnum.THREE_SPLIT_SECTION))));
  }

  private static TextSection text(PageSection.PriorityEnum priority, String text) {
    return new TextSection().text(text).priority(priority).type(PageSection.TypeEnum.TEXT);
  }

  private static ImageSection image(PageSection.PriorityEnum priority, String caption) {
    return new ImageSection()
        .fileId(CUSTOM_PAGE_IMAGE_FILE_ID)
        .caption(caption)
        .priority(priority)
        .type(PageSection.TypeEnum.IMAGE);
  }

  private static TableSection table(
      PageSection.PriorityEnum priority, List<String> headers, List<List<String>> rows) {
    return new TableSection()
        .tableData(new TableData().headers(headers).rows(rows))
        .priority(priority)
        .type(PageSection.TypeEnum.TABLE);
  }

  private static void generatePdf(String payloadPath, String imagePath, String suffix)
      throws IOException {
    renderPdf(readAnnotation(payloadPath), imagePath, suffix);
  }

  private static ExportAreaPictureAnnotation readAnnotation(String payloadPath) throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return TestContextFactories.readPayload(
        objectMapper, new ClassPathResource(payloadPath).getInputStream());
  }

  private static void renderPdf(
      ExportAreaPictureAnnotation exportAreaPictureAnnotation, String imagePath, String suffix)
      throws IOException {
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
