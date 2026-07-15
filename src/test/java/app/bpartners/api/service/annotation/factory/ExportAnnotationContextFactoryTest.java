package app.bpartners.api.service.annotation.factory;

import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64ToUri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.LogCaptor;
import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.model.ImageSection;
import app.bpartners.api.endpoint.rest.model.PageSection.PriorityEnum;
import app.bpartners.api.endpoint.rest.model.PageSection.TypeEnum;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.endpoint.rest.model.TableSection;
import app.bpartners.api.endpoint.rest.model.TextSection;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.annotation.model.custompage.SectionPriority;
import app.bpartners.api.service.file.FileService;
import ch.qos.logback.classic.Level;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.context.Context;

public class ExportAnnotationContextFactoryTest {
  FileService fileService = mock(FileService.class);
  ExportAreaPictureAnnotationImage3DGenerator image3DGenerator =
      new ExportAreaPictureAnnotationImage3DGenerator();
  AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper =
      new AreaPictureAnnotationConfRestMapper();

  @Test
  void configure_3d_pan_image_context() throws IOException {
    File imageFile = new ClassPathResource("files/image-with-vegetation.jpg").getFile();
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan pan = new ExportAreaPictureAnnotation3DPan();
    pan.setImageUri("file-id");
    pan.setPolygon(dummyPolygon(50, 50, 50, 50));
    pan.setName("pan1");
    annotation3D.addPansItem(pan);
    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(imageFile);

    List<String> actual =
        ExportAnnotationContextFactory.getPansImages3DContext(annotation3D, fileService);

    assertNotNull(actual, "Result should not be null");
    assertEquals(1, actual.size());
    String dataUri = actual.get(0);
    assertNotNull(dataUri);
    assertTrue(dataUri.startsWith("data:image/png;base64,"));
  }

  @Test
  void configure_3d_pan_image_context_should_fallback_when_uri_is_blank() {
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan pan = new ExportAreaPictureAnnotation3DPan();
    pan.setImageUri("");
    pan.setPolygon(dummyPolygon(50, 50, 50, 50));
    pan.setName("pan_blank");
    annotation3D.addPansItem(pan);

    List<String> actual =
        ExportAnnotationContextFactory.getPansImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertTrue(actual.get(0).startsWith("data:image/png;base64,"));
  }

  @Test
  void configure_3d_pan_image_context_should_fallback_on_download_io_exception() {
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan pan = new ExportAreaPictureAnnotation3DPan();
    pan.setImageUri("file-id");
    pan.setPolygon(dummyPolygon(50, 50, 50, 50));
    pan.setName("pan_error");
    annotation3D.addPansItem(pan);

    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(null);

    List<String> actual =
        ExportAnnotationContextFactory.getPansImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertTrue(actual.get(0).startsWith("data:image/png;base64,"));
  }

  @Test
  void configure_3d_pan_image_context_should_log_when_file_info_missing() {
    LogCaptor logCaptor = new LogCaptor();
    logCaptor.configure(ExportAnnotationContextFactory.class);

    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan pan = new ExportAreaPictureAnnotation3DPan();
    pan.setImageUri("file-id");
    pan.setPolygon(dummyPolygon(50, 50, 50, 50));
    pan.setName("pan_missing_file_info");
    annotation3D.addPansItem(pan);

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

    assertEquals("data:image/png;base64,abc", result);
  }

  @Test
  void base64_to_uri_should_not_prefix_when_already_uri() {
    String input = "data:image/png;base64,abc";

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
    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setLlm("analysis text");

    ExportAnnotationContextFactory.configureLLMContext(context, annotation);

    assertEquals("analysis text", context.getVariable("llm"));
  }

  @Test
  void configure_global_rate_context_should_add_variables() {
    Context context = new Context();
    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setGlobalRateType("A");
    annotation.setGlobalRateValue(0.5);

    ExportAnnotationContextFactory.configureGlobalRateContext(context, annotation);

    assertEquals("A", context.getVariable("globalRateType"));
    assertEquals(0.5, context.getVariable("globalRateValue"));
    List<?> degradationLevels = (List<?>) context.getVariable("degradationLevels");
    assertEquals(5, degradationLevels.size());
  }

  @Test
  void configure_annotation_3d_context_should_add_3d_variables() {
    Context context = new Context();
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    annotation3D.setPans(
        List.of(
            export3DPan("Pan Est", "25m²", "Bon état", 50, 50, 150, 150),
            export3DPan("Pan Ouest", "22m²", "À rénover", 200, 50, 300, 150)));
    Pair<String, List<String>> images = new Pair<>("main3d", List.of("a", "b"));

    ExportAnnotationContextFactory.configureAnnotation3DContext(
        context, annotation3D, images, fileService);
    assertEquals("data:image/png;base64,main3d", context.getVariable("mainImage3D"));
    List<List<String>> subImagesPages =
        (List<List<String>>) context.getVariable("topViewPanImagesUris");
    assertEquals(1, subImagesPages.size());
    assertEquals(2, subImagesPages.get(0).size());
    assertEquals("data:image/png;base64,a", subImagesPages.get(0).get(0));
    assertEquals("data:image/png;base64,b", subImagesPages.get(0).get(1));

    List<List<ExportAreaPictureAnnotation3DPan>> pages3D =
        (List<List<ExportAreaPictureAnnotation3DPan>>) context.getVariable("pages3D");
    assertFalse(pages3D.isEmpty());
    assertEquals(1, pages3D.size());
    assertEquals(2, pages3D.get(0).size());

    ExportAreaPictureAnnotation3DPan pan1 = pages3D.get(0).get(0);
    assertEquals("Pan Est", pan1.getName());
    assertEquals(2, pan1.getInfos().size());
    assertEquals("Surface", pan1.getInfos().get(0).getLabel());
    assertEquals("25m²", pan1.getInfos().get(0).getValue());
    assertEquals("Observation", pan1.getInfos().get(1).getLabel());
    assertEquals("Bon état", pan1.getInfos().get(1).getValue());

    ExportAreaPictureAnnotation3DPan pan2 = pages3D.get(0).get(1);
    assertEquals("Pan Ouest", pan2.getName());
    assertEquals(2, pan2.getInfos().size());
    assertEquals("Surface", pan2.getInfos().get(0).getLabel());
    assertEquals("22m²", pan2.getInfos().get(0).getValue());
    assertEquals("Observation", pan2.getInfos().get(1).getLabel());
    assertEquals("À rénover", pan2.getInfos().get(1).getValue());

    // Setting up facades
    annotation3D.setFacades(
        List.of(
            export3DPan("Facade 1", "30m²", "Bon état", 50, 50, 150, 150),
            export3DPan("Facade 2", "35m²", "À rénover", 200, 50, 300, 150)));

    Pair<String, List<String>> facadeImages = new Pair<>("main3d", List.of("c", "d"));

    ExportAnnotationContextFactory.configureAnnotationFacade3DContext(
        context, annotation3D, facadeImages, fileService);

    List<List<ExportAreaPictureAnnotation3DPan>> pagesFacade3D =
        (List<List<ExportAreaPictureAnnotation3DPan>>) context.getVariable("pagesFacade3D");
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
    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setAddress("Paris");
    annotation.setAnnotations(List.of());
    Pair<String, List<String>> images = new Pair<>("main", List.of("sub1", "sub2"));
    Pair<String, List<String>> images3d = new Pair<>("main3d", List.of());

    Context context =
        ExportAnnotationContextFactory.createContext(
            user,
            "logo",
            annotation,
            images,
            images3d,
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);

    assertEquals(user, context.getVariable("user"));
    assertEquals("https://example.com", context.getVariable("userWebsite"));
    assertEquals("data:image/png;base64,logo", context.getVariable("logo"));
    assertEquals("Paris", context.getVariable("address"));
    assertEquals("data:image/png;base64,main", context.getVariable("mainImage"));
  }

  @Test
  void create_context_should_handle_null_logo() {
    User user = new User();
    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setAnnotations(List.of());
    Pair<String, List<String>> images = new Pair<>("main", List.of());
    Pair<String, List<String>> images3d = new Pair<>("main3d", List.of());

    Context context =
        ExportAnnotationContextFactory.createContext(
            user,
            null,
            annotation,
            images,
            images3d,
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);

    assertNull(context.getVariable("logo"));
  }

  @Test
  void create_context_should_add_optional_sections() {
    User user = new User();
    user.setAccountHolders(
        List.of(new AccountHolder().toBuilder().website("https://example.com").build()));
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    annotation3D.setPans(List.of());
    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setAnnotations(List.of());
    annotation.setLlm("llm text");
    annotation.setGlobalRateType("B");
    annotation.setGlobalRateValue(0.7);
    annotation.set3d(annotation3D);
    Pair<String, List<String>> images = new Pair<>("main", List.of());
    Pair<String, List<String>> images3d = new Pair<>("main3d", List.of());

    Context context =
        ExportAnnotationContextFactory.createContext(
            user,
            "logo",
            annotation,
            images,
            images3d,
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);

    assertEquals("llm text", context.getVariable("llm"));
    assertEquals("B", context.getVariable("globalRateType"));
    assertEquals(0.7, context.getVariable("globalRateValue"));
    assertNotNull(context.getVariable("mainImage3D"));
  }

  @Test
  void create_context_should_add_custom_pages() {
    User user = new User();
    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setAnnotations(List.of());
    annotation.setAddress("Paris");
    annotation.setImageUrl("http://image.com");

    CustomPage restPage = new CustomPage();
    restPage.setPageTitle("Custom Title");
    TextSection restSection = new TextSection();
    restSection.setType(TypeEnum.TEXT);
    restSection.setPriority(PriorityEnum.IMPORTANT);
    restSection.setText("Custom Text");
    restPage.setSections(List.of(restSection));
    annotation.setCustomPages(List.of(restPage));

    Pair<String, List<String>> images = new Pair<>("main", List.of());
    Pair<String, List<String>> images3d = new Pair<>("main3d", List.of());

    Context context =
        ExportAnnotationContextFactory.createContext(
            user,
            null,
            annotation,
            images,
            images3d,
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);

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
    TextSection restSection = new TextSection();
    restSection.setType(TypeEnum.TEXT);
    restSection.setPriority(PriorityEnum.IMPORTANT);
    restSection.setText("Hello World");

    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setCustomPages(
        List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
    annotation.setAnnotations(List.of());
    annotation.setAddress("Paris");

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    app.bpartners.api.service.annotation.model.custompage.TextSection mapped =
        (app.bpartners.api.service.annotation.model.custompage.TextSection)
            customPages.get(0).getSections().get(0);
    assertEquals("Hello World", mapped.getText());
    assertEquals(SectionPriority.IMPORTANT, mapped.getPriority());
  }

  @Test
  void map_section_should_map_table_section() {
    TableSection restSection = new TableSection();
    restSection.setType(TypeEnum.TABLE);
    restSection.setPriority(PriorityEnum.MEDIUM);
    TableData tableData = new TableData();
    tableData.setHeaders(List.of("H1", "H2"));
    tableData.setRows(List.of(List.of("R1C1", "R1C2")));
    restSection.setTableData(tableData);

    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setCustomPages(
        List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
    annotation.setAnnotations(List.of());
    annotation.setAddress("Paris");

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    app.bpartners.api.service.annotation.model.custompage.TableSection mapped =
        (app.bpartners.api.service.annotation.model.custompage.TableSection)
            customPages.get(0).getSections().get(0);
    assertEquals(SectionPriority.MEDIUM, mapped.getPriority());
    assertEquals(List.of("H1", "H2"), mapped.getTableData().getHeaders());
    assertEquals(List.of("R1C1", "R1C2"), mapped.getTableData().getRows().get(0));
  }

  @Test
  void map_section_should_map_image_section_with_successful_download() {
    ImageSection restSection = new ImageSection();
    restSection.setType(TypeEnum.IMAGE);
    restSection.setPriority(PriorityEnum.SMALL);
    restSection.setUrl(URI.create("https://example.com/image.png"));
    restSection.setCaption("A caption");

    BufferedImage mockImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

    try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class)) {
      mockedImageIO.when(() -> ImageIO.read(Mockito.any(URL.class))).thenReturn(mockImage);

      ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
      annotation.setCustomPages(
          List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
      annotation.setAnnotations(List.of());
      annotation.setAddress("Paris");

      Context context =
          ExportAnnotationContextFactory.createContext(
              new User(),
              null,
              annotation,
              new Pair<>("a", List.of()),
              new Pair<>("b", List.of()),
              fileService,
              image3DGenerator,
              areaPictureAnnotationConfRestMapper);
      List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
          (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
              context.getVariable("customPages");

      app.bpartners.api.service.annotation.model.custompage.ImageSection mapped =
          (app.bpartners.api.service.annotation.model.custompage.ImageSection)
              customPages.get(0).getSections().get(0);
      assertEquals(SectionPriority.SMALL, mapped.getPriority());
      assertEquals("A caption", mapped.getCaption());
      assertTrue(mapped.getUrl().startsWith("data:image/png;base64,"));
    }
  }

  @Test
  void map_section_should_fallback_when_image_download_fails() {
    ImageSection restSection = new ImageSection();
    restSection.setType(TypeEnum.IMAGE);
    restSection.setPriority(PriorityEnum.SMALL);
    restSection.setUrl(URI.create("https://example.com/image.png"));

    try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class)) {
      mockedImageIO
          .when(() -> ImageIO.read(Mockito.any(URL.class)))
          .thenThrow(new IOException("Download failed"));

      ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
      annotation.setCustomPages(
          List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
      annotation.setAnnotations(List.of());
      annotation.setAddress("Paris");

      Context context =
          ExportAnnotationContextFactory.createContext(
              new User(),
              null,
              annotation,
              new Pair<>("a", List.of()),
              new Pair<>("b", List.of()),
              fileService,
              image3DGenerator,
              areaPictureAnnotationConfRestMapper);
      List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
          (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
              context.getVariable("customPages");

      app.bpartners.api.service.annotation.model.custompage.ImageSection mapped =
          (app.bpartners.api.service.annotation.model.custompage.ImageSection)
              customPages.get(0).getSections().get(0);
      assertEquals("https://example.com/image.png", mapped.getUrl());
    }
  }

  @Test
  void map_section_should_fallback_when_image_is_null() {
    ImageSection restSection = new ImageSection();
    restSection.setType(TypeEnum.IMAGE);
    restSection.setPriority(PriorityEnum.SMALL);
    restSection.setUrl(URI.create("https://example.com/image.png"));

    try (MockedStatic<ImageIO> mockedImageIO = Mockito.mockStatic(ImageIO.class)) {
      mockedImageIO.when(() -> ImageIO.read(Mockito.any(URL.class))).thenReturn(null);

      ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
      annotation.setCustomPages(
          List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
      annotation.setAnnotations(List.of());
      annotation.setAddress("Paris");

      Context context =
          ExportAnnotationContextFactory.createContext(
              new User(),
              null,
              annotation,
              new Pair<>("a", List.of()),
              new Pair<>("b", List.of()),
              fileService,
              image3DGenerator,
              areaPictureAnnotationConfRestMapper);
      List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
          (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
              context.getVariable("customPages");

      app.bpartners.api.service.annotation.model.custompage.ImageSection mapped =
          (app.bpartners.api.service.annotation.model.custompage.ImageSection)
              customPages.get(0).getSections().get(0);
      assertEquals("https://example.com/image.png", mapped.getUrl());
    }
  }

  @Test
  void map_section_should_block_non_http_urls() {
    ImageSection restSection = new ImageSection();
    restSection.setType(TypeEnum.IMAGE);
    restSection.setPriority(PriorityEnum.SMALL);
    restSection.setUrl(URI.create("file:///etc/passwd"));

    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setCustomPages(
        List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
    annotation.setAnnotations(List.of());
    annotation.setAddress("Paris");

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    app.bpartners.api.service.annotation.model.custompage.ImageSection mapped =
        (app.bpartners.api.service.annotation.model.custompage.ImageSection)
            customPages.get(0).getSections().get(0);
    assertEquals("file:///etc/passwd", mapped.getUrl());
  }

  @Test
  void map_section_should_handle_invalid_url() {
    ImageSection restSection = new ImageSection();
    restSection.setType(TypeEnum.IMAGE);
    restSection.setPriority(PriorityEnum.SMALL);
    restSection.setUrl(URI.create("not-a-url"));

    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setCustomPages(
        List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
    annotation.setAnnotations(List.of());
    annotation.setAddress("Paris");

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    app.bpartners.api.service.annotation.model.custompage.ImageSection mapped =
        (app.bpartners.api.service.annotation.model.custompage.ImageSection)
            customPages.get(0).getSections().get(0);
    assertEquals("not-a-url", mapped.getUrl());
  }

  @Test
  void map_section_should_throw_on_unknown_section_type() {
    app.bpartners.api.endpoint.rest.model.PageSection unknownSection =
        new app.bpartners.api.endpoint.rest.model.PageSection() {};
    unknownSection.setPriority(PriorityEnum.MEDIUM);

    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setCustomPages(
        List.of(new CustomPage().pageTitle("Title").sections(List.of(unknownSection))));
    annotation.setAnnotations(List.of());
    annotation.setAddress("Paris");

    User user = new User();
    Pair<String, List<String>> pairA = new Pair<>("a", List.of());
    Pair<String, List<String>> pairB = new Pair<>("b", List.of());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            ExportAnnotationContextFactory.createContext(
                user,
                null,
                annotation,
                pairA,
                pairB,
                fileService,
                image3DGenerator,
                areaPictureAnnotationConfRestMapper));
  }

  public static ExportAreaPictureAnnotation3DPan export3DPan(
      String name, String surface, String observation, int x1, int y1, int x2, int y2) {
    return new ExportAreaPictureAnnotation3DPan()
        .name(name)
        .polygon(dummyPolygon(x1, y1, x2, y2))
        .imageUri(null)
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

  @Test
  void map_section_should_map_split_section() {
    SplitSection restSection = new SplitSection();
    restSection.setType(TypeEnum.SPLIT_SECTION);
    restSection.setPriority(PriorityEnum.MEDIUM);

    TextSection leftText = new TextSection();
    leftText.setType(TypeEnum.TEXT);
    leftText.setPriority(PriorityEnum.SMALL);
    leftText.setText("Left");

    TextSection rightText = new TextSection();
    rightText.setType(TypeEnum.TEXT);
    rightText.setPriority(PriorityEnum.SMALL);
    rightText.setText("Right");

    restSection.setLeftSection(leftText);
    restSection.setRightSection(rightText);

    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setCustomPages(
        List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
    annotation.setAnnotations(List.of());
    annotation.setAddress("Paris");

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    app.bpartners.api.service.annotation.model.custompage.SplitSection mapped =
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
    ThreeSplitSection restSection = new ThreeSplitSection();
    restSection.setType(TypeEnum.THREE_SPLIT_SECTION);
    restSection.setPriority(PriorityEnum.MEDIUM);

    TextSection leftText = new TextSection();
    leftText.setType(TypeEnum.TEXT);
    leftText.setPriority(PriorityEnum.SMALL);
    leftText.setText("Left");

    TextSection middleText = new TextSection();
    middleText.setType(TypeEnum.TEXT);
    middleText.setPriority(PriorityEnum.SMALL);
    middleText.setText("Middle");

    TextSection rightText = new TextSection();
    rightText.setType(TypeEnum.TEXT);
    rightText.setPriority(PriorityEnum.SMALL);
    rightText.setText("Right");

    restSection.setLeftSection(leftText);
    restSection.setMiddleSection(middleText);
    restSection.setRightSection(rightText);

    ExportAreaPictureAnnotation annotation = new ExportAreaPictureAnnotation();
    annotation.setCustomPages(
        List.of(new CustomPage().pageTitle("Title").sections(List.of(restSection))));
    annotation.setAnnotations(List.of());
    annotation.setAddress("Paris");

    Context context =
        ExportAnnotationContextFactory.createContext(
            new User(),
            null,
            annotation,
            new Pair<>("a", List.of()),
            new Pair<>("b", List.of()),
            fileService,
            image3DGenerator,
            areaPictureAnnotationConfRestMapper);
    List<app.bpartners.api.service.annotation.model.custompage.CustomPage> customPages =
        (List<app.bpartners.api.service.annotation.model.custompage.CustomPage>)
            context.getVariable("customPages");

    app.bpartners.api.service.annotation.model.custompage.ThreeSplitSection mapped =
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
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan facade = new ExportAreaPictureAnnotation3DPan();
    facade.setImageUri("file-id");
    facade.setPolygon(dummyPolygon(50, 50, 50, 50));
    facade.setName("facade1");
    annotation3D.addFacadesItem(facade);
    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(imageFile);

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual, "Result should not be null");
    assertEquals(1, actual.size());
    String dataUri = actual.get(0);
    assertNotNull(dataUri);
    assertTrue(dataUri.startsWith("data:image/png;base64,"));
  }

  @Test
  void configure_3d_facade_image_context_should_fallback_when_uri_is_blank() {
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan facade = new ExportAreaPictureAnnotation3DPan();
    facade.setImageUri("");
    facade.setPolygon(dummyPolygon(50, 50, 50, 50));
    facade.setName("facade_blank");
    annotation3D.addFacadesItem(facade);

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertTrue(actual.get(0).startsWith("data:image/png;base64,"));
  }

  @Test
  void configure_3d_facade_image_context_should_log_when_file_info_missing() {
    LogCaptor logCaptor = new LogCaptor();
    logCaptor.configure(ExportAnnotationContextFactory.class);

    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan facade = new ExportAreaPictureAnnotation3DPan();
    facade.setImageUri("file-id");
    facade.setPolygon(dummyPolygon(50, 50, 50, 50));
    facade.setName("facade_missing_file_info");
    annotation3D.addFacadesItem(facade);

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
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan facade = new ExportAreaPictureAnnotation3DPan();
    facade.setImageUri("file-id");
    facade.setPolygon(dummyPolygon(50, 50, 50, 50));
    facade.setName("facade_error");
    annotation3D.addFacadesItem(facade);

    when(fileService.findById("file-id"))
        .thenReturn(FileInfo.builder().id("file-id").userUploaderId("user-id").build());
    when(fileService.downloadFile(FileType.IMAGE, "user-id", "file-id")).thenReturn(null);

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertEquals(1, actual.size());
    assertTrue(actual.get(0).startsWith("data:image/png;base64,"));
  }

  @Test
  void configure_3d_facade_image_context_should_fallback_on_io_exception_during_read() {
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan facade = new ExportAreaPictureAnnotation3DPan();
    facade.setImageUri("file-id");
    facade.setPolygon(dummyPolygon(50, 50, 50, 50));
    facade.setName("facade_read_error");
    annotation3D.addFacadesItem(facade);

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
      assertTrue(actual.get(0).startsWith("data:image/png;base64,"));
    }
  }

  @Test
  void configure_3d_facade_image_context_should_return_empty_when_facades_null() {
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    annotation3D.setFacades(null);

    List<String> actual =
        ExportAnnotationContextFactory.getFacadesImages3DContext(annotation3D, fileService);

    assertNotNull(actual);
    assertTrue(actual.isEmpty());
  }

  public static Polygon dummyPolygon(int x1, int y1, int x2, int y2) {
    return new Polygon()
        .points(
            List.of(
                new Point().x((double) x1).y((double) y1),
                new Point().x((double) x2).y((double) y1),
                new Point().x((double) x2).y((double) y2),
                new Point().x((double) x1).y((double) y2),
                new Point().x((double) x1).y((double) y1)));
  }
}
