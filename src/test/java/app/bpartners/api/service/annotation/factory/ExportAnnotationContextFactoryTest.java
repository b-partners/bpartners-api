package app.bpartners.api.service.annotation.factory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.model.Pair;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.context.Context;

public class ExportAnnotationContextFactoryTest {
  BucketComponent bucketComponent = mock();

  @Test
  void configure_3d_pan_image_context() throws IOException {
    File imageFile = new ClassPathResource("files/image-with-vegetation.jpg").getFile();
    ExportAreaPictureAnnotation3D annotation3D = new ExportAreaPictureAnnotation3D();
    ExportAreaPictureAnnotation3DPan pan = new ExportAreaPictureAnnotation3DPan();
    pan.setImageUri(imageFile.getAbsolutePath());
    pan.setPolygon(dummyPolygon(50, 50, 50, 50));
    pan.setName("pan1");
    annotation3D.addPansItem(pan);
    when(bucketComponent.download(any(), anyBoolean())).thenReturn(imageFile);

    List<String> actual =
        ExportAnnotationContextFactory.getPansImages3DContext(annotation3D, bucketComponent);

    assertNotNull(actual, "Result should not be null");
    assertEquals(1, actual.size());
    String dataUri = actual.get(0);
    assertNotNull(dataUri);
    assertTrue(dataUri.startsWith("data:image/png;base64,"));
  }

  @Test
  void base64_to_uri_should_prefix_when_missing() {
    String result = ExportAnnotationContextFactory.base64ToUri("abc");

    assertEquals("data:image/png;base64,abc", result);
  }

  @Test
  void base64_to_uri_should_not_prefix_when_already_uri() {
    String input = "data:image/png;base64,abc";

    String result = ExportAnnotationContextFactory.base64ToUri(input);

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
        context, annotation3D, images, bucketComponent);

    assertEquals("data:image/png;base64,main3d", context.getVariable("mainImage3D"));
    List<List<String>> subImagesPages =
        (List<List<String>>) context.getVariable("subImagesPages3D");
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
            user, "logo", annotation, images, images3d, bucketComponent);

    assertEquals(user, context.getVariable("user"));
    assertEquals("https://example.com", context.getVariable("userWebsite"));
    assertEquals("data:image/png;base64,logo", context.getVariable("logo"));
    assertEquals("Paris", context.getVariable("address"));
    assertEquals("data:image/png;base64,main", context.getVariable("mainImage"));
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
            user, "logo", annotation, images, images3d, bucketComponent);

    assertEquals("llm text", context.getVariable("llm"));
    assertEquals("B", context.getVariable("globalRateType"));
    assertEquals(0.7, context.getVariable("globalRateValue"));
    assertNotNull(context.getVariable("mainImage3D"));
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
