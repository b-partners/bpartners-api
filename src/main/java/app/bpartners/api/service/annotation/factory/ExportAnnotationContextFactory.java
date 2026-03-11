package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.model.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.thymeleaf.context.Context;

public class ExportAnnotationContextFactory {
  private static final String BASE_64_URI_PREFIX = "data:image/png;base64,";

  public static Context createContext(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages) {
    var context = new Context();

    var logoUri = base64ToUri(logoBase64);
    var mainImageUri = base64ToUri(annotationImages.first());
    var subImagesUris =
        annotationImages.second().stream()
            .map(ExportAnnotationContextFactory::base64ToUri)
            .toList();

    context.setVariable("user", user);
    context.setVariable("userWebsite", user.getDefaultWebsite());
    context.setVariable("logo", logoUri);
    context.setVariable("address", annotation.getAddress());
    context.setVariable("mainImage", mainImageUri);
    context.setVariable("subImages", subImagesUris);
    context.setVariable(
        "pages",
        groupByFirstPage(
            ExportAreaPictureAnnotationPDFGenerator.GroupedByKey.from(annotation.getAnnotations()),
            3,
            3));

    if (annotation.getLlm() != null) {
      configureLLMContext(context, annotation);
    }

    if (annotation.getGlobalRateValue() != null || annotation.getGlobalRateType() != null) {
      configureGlobalRateContext(context, annotation);
    }
    if (annotation.get3d() != null) {
      configureAnnotation3DContext(context, annotation.get3d(), annotation3DImages);
    }

    return context;
  }

  static void configureLLMContext(Context context, ExportAreaPictureAnnotation annotation) {
    context.setVariable("llm", annotation.getLlm());
  }

  static void configureGlobalRateContext(Context context, ExportAreaPictureAnnotation annotation) {
    context.setVariable("globalRateType", annotation.getGlobalRateType());
    context.setVariable("globalRateValue", annotation.getGlobalRateValue());
    context.setVariable(
        "degradationLevels",
        List.of(
            Map.of("label", "A", "color", "#47BE62"),
            Map.of("label", "B", "color", "#F4FBAB"),
            Map.of("label", "C", "color", "#F9DD56"),
            Map.of("label", "D", "color", "#F38F4B"),
            Map.of("label", "E", "color", "#EF2C2D")));
  }

  static void configureAnnotation3DContext(
      Context context,
      ExportAreaPictureAnnotation3D annotation3D,
      Pair<String, List<String>> annotation3DImages) {
    var pages = groupByFirstPage(annotation3D.getPans(), 3, 4);
    var mainImage3DUri = base64ToUri(annotation3DImages.first());
    var panImages3DUris =
        annotation3DImages.second().stream()
            .map(ExportAnnotationContextFactory::base64ToUri)
            .toList();

    context.setVariable("pages3D", pages);
    context.setVariable("mainImage3D", mainImage3DUri);
    context.setVariable("subImages3D", panImages3DUris);
  }

  static <T> List<List<T>> groupByFirstPage(List<T> list, int firstPageMax, int limit) {
    List<List<T>> pages = new ArrayList<>();
    var iterator = list.iterator();

    List<T> firstPage = new ArrayList<>();
    for (int i = 0; i < firstPageMax && iterator.hasNext(); i++) {
      firstPage.add(iterator.next());
    }
    if (!firstPage.isEmpty()) {
      pages.add(firstPage);
    }

    while (iterator.hasNext()) {
      List<T> page = new ArrayList<>();
      for (int i = 0; i < limit && iterator.hasNext(); i++) {
        page.add(iterator.next());
      }
      pages.add(page);
    }

    return pages;
  }

  static String base64ToUri(String base64Image) {
    return !base64Image.startsWith("data:") ? BASE_64_URI_PREFIX + base64Image : base64Image;
  }
}
