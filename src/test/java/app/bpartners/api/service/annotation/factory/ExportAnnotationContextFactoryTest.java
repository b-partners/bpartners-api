package app.bpartners.api.service.annotation.factory;

import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64ToUri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.LogCaptor;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.AreaAnnotation3D;
import app.bpartners.api.service.annotation.AreaAnnotation3DPan;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.AreaAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.AreaAnnotationMeasurement;
import app.bpartners.api.service.annotation.Point;
import app.bpartners.api.service.annotation.Polygon;
import app.bpartners.api.service.annotation.export.AreaAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.annotation.model.custompage.CustomPage;
import app.bpartners.api.service.annotation.model.custompage.ImageSection;
import app.bpartners.api.service.annotation.model.custompage.SectionPriority;
import app.bpartners.api.service.annotation.model.custompage.SplitSection;
import app.bpartners.api.service.annotation.model.custompage.TableData;
import app.bpartners.api.service.annotation.model.custompage.TableSection;
import app.bpartners.api.service.annotation.model.custompage.TextSection;
import app.bpartners.api.service.annotation.model.custompage.ThreeSplitSection;
import app.bpartners.api.service.file.FileService;
import ch.qos.logback.classic.Level;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.context.Context;

public class ExportAnnotationContextFactoryTest {
  FileService fileService = mock(FileService.class);
  AreaAnnotationImage3DGenerator image3DGenerator = new AreaAnnotationImage3DGenerator();

  @Test
  void configure_3d_pan_image_context() throws IOException {
    File imageFile = new ClassPathResource("files/image-with-vegetation.jpg").getFile();
    var annotation3D =
        AreaAnnotation3D.builder()
            .pans(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("file-id")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("pan1")
                        .build()))
            .build();
    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(imageFile);

    List<String> actual =
        ExportAnnotationContextFactory.getPansImages3DContext(annotation3D, fileService);

    assertNotNull(actual, "Result should not be null");
    assertEquals(1, actual.size());
    String dataUri = actual.get(0);
    assertNotNull(dataUri);
    assertTrue(dataUri.startsWith("data:image/jpeg;base64,"));
  }

  @Test
  void configure_3d_pan_image_context_should_fallback_when_uri_is_blank() {
    var annotation3D =
        AreaAnnotation3D.builder()
            .pans(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("pan_blank")
                        .build()))
            .build();

    List<String> actual =
        ExportAnnotationContextFactory.getPansImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertTrue(actual.get(0).startsWith("data:image/jpeg;base64,"));
  }

  @Test
  void configure_3d_pan_image_context_should_fallback_on_download_io_exception() {
    var annotation3D =
        AreaAnnotation3D.builder()
            .pans(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("file-id")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("pan_error")
                        .build()))
            .build();

    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(null);

    List<String> actual =
        ExportAnnotationContextFactory.getPansImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertTrue(actual.get(0).startsWith("data:image/jpeg;base64,"));
  }

  @Test
  void configure_3d_pan_image_context_should_log_when_file_info_missing() {
    LogCaptor logCaptor = new LogCaptor();
    logCaptor.configure(ExportAnnotationContextFactory.class);

    var annotation3D =
        AreaAnnotation3D.builder()
            .pans(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("file-id")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("pan_missing_file_info")
                        .build()))
            .build();

    when(fileService.findById("file-id")).thenReturn(null);

    List<String> actual =
        ExportAnnotationContextFactory.getPansImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());

    var warnEvents =
        logCaptor.getLogEvents().stream()
            .filter(event -> event.getLevel().equals(Level.WARN))
            .toList();
    assertEquals(1, warnEvents.size());
    assertTrue(
        warnEvents
            .get(0)
            .getFormattedMessage()
            .contains("Can't get image file for pan: pan_missing_file_info"));
    assertTrue(warnEvents.get(0).getFormattedMessage().contains("file-id"));
  }

  @Test
  void base64_to_uri_should_prefix_when_missing() {
    String result = base64ToUri("abc");

    assertEquals("data:image/jpeg;base64,abc", result);
  }

  @Test
  void base64_to_uri_should_not_prefix_when_already_uri() {
    String input = "data:image/jpeg;base64,abc";

    String result = base64ToUri(input);

    assertEquals(input, result);
  }

  @Test
  void group_by_first_page_should_split_correctly() {
    List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7);

    List<List<Integer>> pages = ExportAnnotationContextFactory.groupByFirstPage(list, 3, 3);

    assertEquals(3, pages.size());
    assertEquals(List.of(1, 2, 3), pages.get(0));
    assertEquals(List.of(4, 5, 6), pages.get(1));
    assertEquals(List.of(7), pages.get(2));
  }

  @Test
  void group_by_first_page_should_return_empty_when_list_empty() {
    List<List<Integer>> pages = ExportAnnotationContextFactory.groupByFirstPage(List.of(), 3, 3);
    assertTrue(pages.isEmpty());
  }

  @Test
  void configure_llm_context_should_add_variable() {
    Context context = new Context();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("addr")
            .imageUrl("url")
            .llm("analysis text")
            .build();

    ExportAnnotationContextFactory.configureLLMContext(context, annotation);

    assertEquals("analysis text", context.getVariable("llm"));
  }

  @Test
  void configure_global_rate_context_should_add_variables() {
    Context context = new Context();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("addr")
            .imageUrl("url")
            .globalRateType("A")
            .globalRateValue(0.5)
            .build();

    ExportAnnotationContextFactory.configureGlobalRateContext(context, annotation);

    assertEquals("A", context.getVariable("globalRateType"));
    assertEquals(0.5, context.getVariable("globalRateValue"));
    List<?> degradationLevels = (List<?>) context.getVariable("degradationLevels");
    assertEquals(5, degradationLevels.size());
  }

  @Test
  void configure_annotation_3d_context_should_add_3d_variables() {
    Context context = new Context();
    var annotation3D =
        AreaAnnotation3D.builder()
            .pans(
                List.of(
                    export3DPan("Pan Est", "25m²", "Bon état", 50, 50, 150, 150),
                    export3DPan("Pan Ouest", "22m²", "À rénover", 200, 50, 300, 150)))
            .build();
    var images = new Pair<String, List<String>>("main3d", List.of("a", "b"));

    ExportAnnotationContextFactory.configureAnnotation3DContext(
        context, annotation3D, images, fileService);
    assertEquals("data:image/jpeg;base64,main3d", context.getVariable("mainImage3D"));
    List<List<String>> subImagesPages =
        (List<List<String>>) context.getVariable("topViewPanImagesUris");
    assertEquals(1, subImagesPages.size());
    assertEquals(2, subImagesPages.get(0).size());
    assertEquals("data:image/jpeg;base64,a", subImagesPages.get(0).get(0));
    assertEquals("data:image/jpeg;base64,b", subImagesPages.get(0).get(1));

    List<List<AreaAnnotation3DPan>> pages3D =
        (List<List<AreaAnnotation3DPan>>) context.getVariable("pages3D");
    assertFalse(pages3D.isEmpty());
    assertEquals(1, pages3D.size());
    assertEquals(2, pages3D.get(0).size());

    var pan1 = pages3D.get(0).get(0);
    assertEquals("Pan Est", pan1.getName());
    assertEquals(2, pan1.getInfos().size());
    assertEquals("Surface", pan1.getInfos().get(0).label());
    assertEquals("25m²", pan1.getInfos().get(0).value());
    assertEquals("Observation", pan1.getInfos().get(1).label());
    assertEquals("Bon état", pan1.getInfos().get(1).value());

    var pan2 = pages3D.get(0).get(1);
    assertEquals("Pan Ouest", pan2.getName());
    assertEquals(2, pan2.getInfos().size());
    assertEquals("Surface", pan2.getInfos().get(0).label());
    assertEquals("22m²", pan2.getInfos().get(0).value());
    assertEquals("Observation", pan2.getInfos().get(1).label());
    assertEquals("À rénover", pan2.getInfos().get(1).value());

    // Setting up facades
    var annotation3DWithFacades =
        annotation3D.toBuilder()
            .facades(
                List.of(
                    export3DPan("Facade 1", "30m²", "Bon état", 50, 50, 150, 150),
                    export3DPan("Facade 2", "35m²", "À rénover", 200, 50, 300, 150)))
            .build();

    var facadeImages = new Pair<String, List<String>>("main3d", List.of("c", "d"));

    ExportAnnotationContextFactory.configureAnnotationFacade3DContext(
        context, annotation3DWithFacades, facadeImages, fileService);

    List<List<AreaAnnotation3DPan>> pagesFacade3D =
        (List<List<AreaAnnotation3DPan>>) context.getVariable("pagesFacade3D");
    assertNotNull(pagesFacade3D);
    assertEquals(1, pagesFacade3D.size());
    assertEquals(2, pagesFacade3D.get(0).size());
    assertEquals("Facade 1", pagesFacade3D.get(0).get(0).getName());
    assertEquals("Facade 2", pagesFacade3D.get(0).get(1).getName());
  }

  @Test
  void create_context_should_populate_main_fields() {
    User user = new User();
    user.setAccountHolders(
        List.of(new AccountHolder().toBuilder().website("https://example.com").build()));
    var annotation = AreaAnnotationExportPayload.builder().address("Paris").imageUrl("url").build();
    var images = new Pair<String, List<String>>("main", List.of("sub1", "sub2"));
    var images3d = new Pair<String, List<String>>("main3d", List.of());

    Context context =
        ExportAnnotationContextFactory.createContext(
            user, "logo", annotation, images, images3d, fileService, image3DGenerator);

    assertEquals(user, context.getVariable("user"));
    assertEquals("https://example.com", context.getVariable("userWebsite"));
    assertEquals("data:image/jpeg;base64,logo", context.getVariable("logo"));
    assertEquals("Paris", context.getVariable("address"));
    assertEquals("data:image/jpeg;base64,main", context.getVariable("mainImage"));
  }

  @Test
  void create_context_should_handle_null_logo() {
    User user = new User();
    var annotation = AreaAnnotationExportPayload.builder().address("addr").imageUrl("url").build();
    var images = new Pair<String, List<String>>("main", List.of());
    var images3d = new Pair<String, List<String>>("main3d", List.of());

    Context context =
        ExportAnnotationContextFactory.createContext(
            user, null, annotation, images, images3d, fileService, image3DGenerator);

    assertNull(context.getVariable("logo"));
  }

  @Test
  void create_context_should_add_optional_sections() {
    User user = new User();
    user.setAccountHolders(
        List.of(new AccountHolder().toBuilder().website("https://example.com").build()));
    var annotation3D = AreaAnnotation3D.builder().build();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("addr")
            .imageUrl("url")
            .llm("llm text")
            .globalRateType("B")
            .globalRateValue(0.7)
            .annotation3d(annotation3D)
            .build();
    var images = new Pair<String, List<String>>("main", List.of());
    var images3d = new Pair<String, List<String>>("main3d", List.of());

    Context context =
        ExportAnnotationContextFactory.createContext(
            user, "logo", annotation, images, images3d, fileService, image3DGenerator);

    assertEquals("llm text", context.getVariable("llm"));
    assertEquals("B", context.getVariable("globalRateType"));
    assertEquals(0.7, context.getVariable("globalRateValue"));
    assertNotNull(context.getVariable("mainImage3D"));
  }

  @Test
  void create_context_should_add_custom_pages() {
    User user = new User();
    var customPage =
        CustomPage.builder()
            .pageTitle("Custom Title")
            .sections(
                List.of(
                    TextSection.builder()
                        .priority(SectionPriority.IMPORTANT)
                        .text("Custom Text")
                        .build()))
            .build();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("Paris")
            .imageUrl("http://image.com")
            .customPages(List.of(customPage))
            .build();
    var images = new Pair<String, List<String>>("main", List.of());
    var images3d = new Pair<String, List<String>>("main3d", List.of());

    Context context =
        ExportAnnotationContextFactory.createContext(
            user, null, annotation, images, images3d, fileService, image3DGenerator);

    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");
    assertNotNull(customPages);
    assertEquals(1, customPages.size());
    assertEquals("Custom Title", customPages.get(0).getPageTitle());
    assertEquals(1, customPages.get(0).getSections().size());
    assertTrue(
        customPages.get(0).getSections().get(0)
            instanceof app.bpartners.api.service.annotation.model.custompage.TextSection);
    assertEquals(
        "Custom Text",
        ((app.bpartners.api.service.annotation.model.custompage.TextSection)
                customPages.get(0).getSections().get(0))
            .getText());
  }

  @Test
  void map_section_should_map_text_section() {
    var textSection =
        TextSection.builder().priority(SectionPriority.IMPORTANT).text("Hello World").build();
    var customPage = CustomPage.builder().pageTitle("Title").sections(List.of(textSection)).build();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("Paris")
            .imageUrl("url")
            .customPages(List.of(customPage))
            .build();

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    var mapped =
        (app.bpartners.api.service.annotation.model.custompage.TextSection)
            customPages.get(0).getSections().get(0);
    assertEquals("Hello World", mapped.getText());
    assertEquals(SectionPriority.IMPORTANT, mapped.getPriority());
  }

  @Test
  void map_section_should_map_table_section() {
    var tableSection =
        TableSection.builder()
            .priority(SectionPriority.MEDIUM)
            .tableData(
                TableData.builder()
                    .headers(List.of("H1", "H2"))
                    .rows(List.of(List.of("R1C1", "R1C2")))
                    .build())
            .build();
    var customPage =
        CustomPage.builder().pageTitle("Title").sections(List.of(tableSection)).build();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("Paris")
            .imageUrl("url")
            .customPages(List.of(customPage))
            .build();

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    var mapped =
        (app.bpartners.api.service.annotation.model.custompage.TableSection)
            customPages.get(0).getSections().get(0);
    assertEquals(SectionPriority.MEDIUM, mapped.getPriority());
    assertEquals(List.of("H1", "H2"), mapped.getTableData().getHeaders());
    assertEquals(List.of("R1C1", "R1C2"), mapped.getTableData().getRows().get(0));
  }

  @Test
  void map_section_should_map_image_section_with_successful_download() {
    BufferedImage mockImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

    try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class)) {
      mockedImageIO.when(() -> ImageIO.read(Mockito.any(java.net.URL.class))).thenReturn(mockImage);

      var customPage =
          CustomPage.builder()
              .pageTitle("Title")
              .sections(
                  List.of(
                      ImageSection.builder()
                          .url("https://example.com/image.png")
                          .caption("A caption")
                          .priority(SectionPriority.SMALL)
                          .build()))
              .build();
      var annotation =
          AreaAnnotationExportPayload.builder()
              .address("Paris")
              .imageUrl("url")
              .customPages(List.of(customPage))
              .build();

      Context context =
          ExportAnnotationContextFactory.createContext(
              new User(),
              null,
              annotation,
              new Pair<>("a", List.of()),
              new Pair<>("b", List.of()),
              fileService,
              image3DGenerator);
      List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
          (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
              context.getVariable("customPages");

      var mapped =
          (app.bpartners.api.service.annotation.model.custompage.ImageSection)
              customPages.get(0).getSections().get(0);
      assertEquals(SectionPriority.SMALL, mapped.getPriority());
      assertEquals("A caption", mapped.getCaption());
      assertTrue(mapped.getUrl().startsWith("data:image/jpeg;base64,"));
    }
  }

  @Test
  void map_section_should_fallback_when_image_download_fails() {
    try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class)) {
      mockedImageIO
          .when(() -> ImageIO.read(Mockito.any(java.net.URL.class)))
          .thenThrow(new IOException("Download failed"));

      var customPage =
          CustomPage.builder()
              .pageTitle("Title")
              .sections(
                  List.of(
                      ImageSection.builder()
                          .url("https://example.com/image.png")
                          .priority(SectionPriority.SMALL)
                          .build()))
              .build();
      var annotation =
          AreaAnnotationExportPayload.builder()
              .address("Paris")
              .imageUrl("url")
              .customPages(List.of(customPage))
              .build();

      Context context =
          ExportAnnotationContextFactory.createContext(
              new User(),
              null,
              annotation,
              new Pair<>("a", List.of()),
              new Pair<>("b", List.of()),
              fileService,
              image3DGenerator);
      List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
          (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
              context.getVariable("customPages");

      var mapped =
          (app.bpartners.api.service.annotation.model.custompage.ImageSection)
              customPages.get(0).getSections().get(0);
      assertEquals("https://example.com/image.png", mapped.getUrl());
    }
  }

  @Test
  void map_section_should_fallback_when_image_is_null() {
    try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class)) {
      mockedImageIO.when(() -> ImageIO.read(Mockito.any(java.net.URL.class))).thenReturn(null);

      var customPage =
          CustomPage.builder()
              .pageTitle("Title")
              .sections(
                  List.of(
                      ImageSection.builder()
                          .url("https://example.com/image.png")
                          .priority(SectionPriority.SMALL)
                          .build()))
              .build();
      var annotation =
          AreaAnnotationExportPayload.builder()
              .address("Paris")
              .imageUrl("url")
              .customPages(List.of(customPage))
              .build();

      Context context =
          ExportAnnotationContextFactory.createContext(
              new User(),
              null,
              annotation,
              new Pair<>("a", List.of()),
              new Pair<>("b", List.of()),
              fileService,
              image3DGenerator);
      List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
          (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
              context.getVariable("customPages");

      var mapped =
          (app.bpartners.api.service.annotation.model.custompage.ImageSection)
              customPages.get(0).getSections().get(0);
      assertEquals("https://example.com/image.png", mapped.getUrl());
    }
  }

  @Test
  void map_section_should_block_non_http_urls() {
    var customPage =
        CustomPage.builder()
            .pageTitle("Title")
            .sections(
                List.of(
                    ImageSection.builder()
                        .url("file:///etc/passwd")
                        .priority(SectionPriority.SMALL)
                        .build()))
            .build();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("Paris")
            .imageUrl("url")
            .customPages(List.of(customPage))
            .build();

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    var mapped =
        (app.bpartners.api.service.annotation.model.custompage.ImageSection)
            customPages.get(0).getSections().get(0);
    assertEquals("file:///etc/passwd", mapped.getUrl());
  }

  @Test
  void map_section_should_handle_invalid_url() {
    var customPage =
        CustomPage.builder()
            .pageTitle("Title")
            .sections(
                List.of(
                    ImageSection.builder()
                        .url("not-a-url")
                        .priority(SectionPriority.SMALL)
                        .build()))
            .build();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("Paris")
            .imageUrl("url")
            .customPages(List.of(customPage))
            .build();

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    var mapped =
        (app.bpartners.api.service.annotation.model.custompage.ImageSection)
            customPages.get(0).getSections().get(0);
    assertEquals("not-a-url", mapped.getUrl());
  }

  public static AreaAnnotation3DPan export3DPan(
      String name, String surface, String observation, int x1, int y1, int x2, int y2) {
    return AreaAnnotation3DPan.builder()
        .name(name)
        .polygon(dummyPolygon(x1, y1, x2, y2))
        .imageUri(null)
        .measurements(
            List.of(
                new AreaAnnotationMeasurement("m", 5.0, false),
                new AreaAnnotationMeasurement("m", 5.0, false),
                new AreaAnnotationMeasurement("m", 5.0, false),
                new AreaAnnotationMeasurement("m", 5.0, false)))
        .infos(
            List.of(
                new AreaAnnotationInstanceInfo("Surface", surface),
                new AreaAnnotationInstanceInfo("Observation", observation)))
        .build();
  }

  @Test
  void map_section_should_map_split_section() {
    var splitSection =
        new SplitSection(
            SectionPriority.MEDIUM,
            TextSection.builder().priority(SectionPriority.SMALL).text("Left").build(),
            TextSection.builder().priority(SectionPriority.SMALL).text("Right").build());
    var customPage =
        CustomPage.builder().pageTitle("Title").sections(List.of(splitSection)).build();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("Paris")
            .imageUrl("url")
            .customPages(List.of(customPage))
            .build();

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    var mapped =
        (app.bpartners.api.service.annotation.model.custompage.SplitSection)
            customPages.get(0).getSections().get(0);
    assertEquals(SectionPriority.MEDIUM, mapped.getPriority());
    assertEquals(
        "Left",
        ((app.bpartners.api.service.annotation.model.custompage.TextSection)
                mapped.getLeftSection())
            .getText());
    assertEquals(
        "Right",
        ((app.bpartners.api.service.annotation.model.custompage.TextSection)
                mapped.getRightSection())
            .getText());
  }

  @Test
  void map_section_should_map_three_split_section() {
    var threeSplitSection =
        new ThreeSplitSection(
            SectionPriority.MEDIUM,
            TextSection.builder().priority(SectionPriority.SMALL).text("Left").build(),
            TextSection.builder().priority(SectionPriority.SMALL).text("Middle").build(),
            TextSection.builder().priority(SectionPriority.SMALL).text("Right").build());
    var customPage =
        CustomPage.builder().pageTitle("Title").sections(List.of(threeSplitSection)).build();
    var annotation =
        AreaAnnotationExportPayload.builder()
            .address("Paris")
            .imageUrl("url")
            .customPages(List.of(customPage))
            .build();

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    var mapped =
        (app.bpartners.api.service.annotation.model.custompage.ThreeSplitSection)
            customPages.get(0).getSections().get(0);
    assertEquals(SectionPriority.MEDIUM, mapped.getPriority());
    assertEquals(
        "Left",
        ((app.bpartners.api.service.annotation.model.custompage.TextSection)
                mapped.getLeftSection())
            .getText());
    assertEquals(
        "Middle",
        ((app.bpartners.api.service.annotation.model.custompage.TextSection)
                mapped.getMiddleSection())
            .getText());
    assertEquals(
        "Right",
        ((app.bpartners.api.service.annotation.model.custompage.TextSection)
                mapped.getRightSection())
            .getText());
  }

  @Test
  void configure_3d_facade_image_context() throws IOException {
    File imageFile = new ClassPathResource("files/image-with-vegetation.jpg").getFile();
    var annotation3D =
        AreaAnnotation3D.builder()
            .facades(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("file-id")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("facade1")
                        .build()))
            .build();
    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(imageFile);

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual, "Result should not be null");
    assertEquals(1, actual.size());
    String dataUri = actual.get(0);
    assertNotNull(dataUri);
    assertTrue(dataUri.startsWith("data:image/jpeg;base64,"));
  }

  @Test
  void configure_3d_facade_image_context_should_fallback_when_uri_is_blank() {
    var annotation3D =
        AreaAnnotation3D.builder()
            .facades(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("facade_blank")
                        .build()))
            .build();

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertTrue(actual.get(0).startsWith("data:image/jpeg;base64,"));
  }

  @Test
  void configure_3d_facade_image_context_should_log_when_file_info_missing() {
    LogCaptor logCaptor = new LogCaptor();
    logCaptor.configure(ExportAnnotationContextFactory.class);

    var annotation3D =
        AreaAnnotation3D.builder()
            .facades(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("file-id")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("facade_missing_file_info")
                        .build()))
            .build();

    when(fileService.findById("file-id")).thenReturn(null);

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());

    var warnEvents =
        logCaptor.getLogEvents().stream()
            .filter(event -> event.getLevel().equals(Level.WARN))
            .toList();
    assertEquals(1, warnEvents.size());
    assertTrue(
        warnEvents
            .get(0)
            .getFormattedMessage()
            .contains("Can't get image file for facade: facade_missing_file_info"));
    assertTrue(warnEvents.get(0).getFormattedMessage().contains("file-id"));
  }

  @Test
  void configure_3d_facade_image_context_should_fallback_on_download_failure() {
    var annotation3D =
        AreaAnnotation3D.builder()
            .facades(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("file-id")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("facade_error")
                        .build()))
            .build();

    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(null);

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertTrue(actual.get(0).startsWith("data:image/jpeg;base64,"));
  }

  @Test
  void configure_3d_facade_image_context_should_fallback_on_io_exception_during_read() {
    var annotation3D =
        AreaAnnotation3D.builder()
            .facades(
                List.of(
                    AreaAnnotation3DPan.builder()
                        .imageUri("file-id")
                        .polygon(dummyPolygon(50, 50, 50, 50))
                        .name("facade_read_error")
                        .build()))
            .build();

    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());

    File imageFile = mock(File.class);
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(imageFile);

    try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class)) {
      mockedImageIO.when(() -> ImageIO.read(imageFile)).thenThrow(new IOException("Read error"));

      List<String> actual =
          ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

      assertNotNull(actual);
      assertEquals(1, actual.size());
      assertTrue(actual.get(0).startsWith("data:image/jpeg;base64,"));
    }
  }

  @Test
  void configure_3d_facade_image_context_should_return_empty_when_facades_null() {
    var annotation3D = AreaAnnotation3D.builder().build();

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertTrue(actual.isEmpty());
  }

  public static Polygon dummyPolygon(int x1, int y1, int x2, int y2) {
    return new Polygon(
        List.of(
            new Point(x1, y1),
            new Point(x2, y1),
            new Point(x2, y2),
            new Point(x1, y2),
            new Point(x1, y1)));
  }
}
