package app.bpartners.api.unit.service;

import static app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactoryTest.export3DPan;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;

class ExportAreaPictureAnnotationPdfContentTest {
  private final TemplateResolverEngine templateResolverEngine = new TemplateResolverEngine();
  private final FileService fileService = mock();
  private final ExportAreaPictureAnnotationImage3DGenerator image3DGenerator =
      new ExportAreaPictureAnnotationImage3DGenerator();

  @BeforeEach
  void setup() {
    when(fileService.downloadFile(any(), any(), any())).thenReturn(null);
  }

  @Test
  void html_should_contain_3d_pans_information() {
    ExportAreaPictureAnnotation annotation =
        new ExportAreaPictureAnnotation()
            .address("123 Test Street")
            ._3d(
                new ExportAreaPictureAnnotation3D()
                    .pans(
                        List.of(
                            export3DPan("Pan Est", "25m²", "Bon état", 50, 50, 150, 150),
                            export3DPan("Pan Ouest", "22m²", "À rénover", 200, 50, 300, 150))));

    Pair<String, List<String>> annotationImages = new Pair<>("main", List.of());
    Pair<String, List<String>> annotation3DImages =
        new Pair<>("main3d", List.of("pan1_img", "pan2_img"));

    Context context =
        app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactory.createContext(
            user(),
            "logo",
            annotation,
            annotationImages,
            annotation3DImages,
            fileService,
            image3DGenerator);

    String html =
        templateResolverEngine
            .getTemplateEngine()
            .process("export-area-picture-annotations", context);

    assertNotNull(html);
    assertTrue(html.contains("Informations détaillées sur les pans du bâtiment"));
    assertTrue(html.contains("Pan Est"));
    assertTrue(html.contains("25m²"));
    assertTrue(html.contains("Bon état"));
    assertTrue(html.contains("Pan Ouest"));
    assertTrue(html.contains("22m²"));
    assertTrue(html.contains("À rénover"));

    // Check for images
    assertTrue(html.contains("data:image/png;base64,main3d"));
    assertTrue(html.contains("data:image/png;base64,pan1_img"));
    assertTrue(html.contains("data:image/png;base64,pan2_img"));
  }

  @Test
  void html_should_contain_llm_analysis() {
    ExportAreaPictureAnnotation annotation =
        new ExportAreaPictureAnnotation()
            .address("123 Test Street")
            .llm("<h2>Analyse LLM</h2><p>Test content with emoji 🛠️</p>");

    Context context =
        app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactory.createContext(
            user(),
            "logo",
            annotation,
            new Pair<>("main", List.of()),
            null,
            fileService,
            image3DGenerator);

    String html =
        templateResolverEngine
            .getTemplateEngine()
            .process("export-area-picture-annotations", context);

    assertNotNull(html);
    assertTrue(html.contains("Analyse LLM"));
    assertTrue(html.contains("Test content with emoji"));
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
