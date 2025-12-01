package app.bpartners.api.service.annotation;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Component
public class ExportAreaPictureAnnotationPDFGenerator {
  private final TemplateResolverEngine templateResolverEngine;
  private final EmojiReplacer emojiReplacer;

  public static final String KEY_LABEL = "key";
  public static final String FONT_NAME = "Kumbh Sans";
  private static final String EMOJI_PATH = "fonts/twemoji/v/14.0.2/svg";
  private static final String BASE_64_URI_PREFIX = "data:image/png;base64,";
  private static final String FONT_PATH = "fonts/KumbhSans-VariableFont_YOPQ,wght.ttf";
  private static final String AREA_PICTURE_ANNOTATION_TEMPLATE = "export-area-picture-annotations";

  public ExportAreaPictureAnnotationPDFGenerator(TemplateResolverEngine templateResolverEngine) {
    this.templateResolverEngine = templateResolverEngine;
    this.emojiReplacer = getEmojiReplacer();
  }

  @SneakyThrows
  public byte[] apply(
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages) {

    var html = parseDataToString(annotation, annotationImages, annotation3DImages);
    if (annotation.getLlm() != null) {
      html = emojiReplacer.replaceEmoji(html);
    }

    try (var outputStream = new ByteArrayOutputStream()) {
      var builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html, null);
      builder.useSVGDrawer(new BatikSVGDrawer());
      builder.toStream(outputStream);
      builder.useFont(new ClassPathResource(FONT_PATH).getFile(), FONT_NAME);
      builder.run();

      return outputStream.toByteArray();
    } catch (RuntimeException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private String parseDataToString(
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages) {
    var templateEngine = templateResolverEngine.getTemplateEngine();
    var context = createContext(annotation, annotationImages, annotation3DImages);
    return templateEngine.process(AREA_PICTURE_ANNOTATION_TEMPLATE, context);
  }

  private Context createContext(
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages) {
    var context = new Context();

    var mainImageUri = base64ToUri(annotationImages.first());
    var subImagesUris =
        annotationImages.second().stream()
            .map(ExportAreaPictureAnnotationPDFGenerator::base64ToUri)
            .toList();

    context.setVariable("address", annotation.getAddress());
    context.setVariable("mainImage", mainImageUri);
    context.setVariable("subImages", subImagesUris);
    context.setVariable(
        "pages", groupByFirstPage(GroupedByKey.from(annotation.getAnnotations()), 3));

    if (annotation.getLlm() != null) {
      configureLLMContext(context, annotation);
    }
    if (annotation.get3d() != null) {
      configureAnnotation3DContext(context, annotation.get3d(), annotation3DImages);
    }

    return context;
  }

  private static void configureLLMContext(Context context, ExportAreaPictureAnnotation annotation) {
    context.setVariable("llm", annotation.getLlm());
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

  private static void configureAnnotation3DContext(
      Context context,
      ExportAreaPictureAnnotation3D annotation3D,
      Pair<String, List<String>> annotation3DImages) {
    var pages = groupByFirstPage(annotation3D.getPans(), 2);
    var mainImage3DUri = base64ToUri(annotation3DImages.first());
    var panImages3DUris =
        annotation3DImages.second().stream()
            .map(ExportAreaPictureAnnotationPDFGenerator::base64ToUri)
            .toList();

    context.setVariable("pages3D", pages);
    context.setVariable("mainImage3D", mainImage3DUri);
    context.setVariable("subImages3D", panImages3DUris);
  }

  public static <T> List<List<T>> groupByFirstPage(List<T> list, int firstPageMax) {
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
      for (int i = 0; i < 3 && iterator.hasNext(); i++) {
        page.add(iterator.next());
      }
      pages.add(page);
    }

    return pages;
  }

  public record GroupedByKey(String key, List<ExportAreaPictureAnnotationInstance> instances) {
    // Used by the template
    public ExportAreaPictureAnnotationInstance mergedInstance() {
      assert !this.instances.isEmpty();
      var instance = this.instances.getFirst();

      return new ExportAreaPictureAnnotationInstance()
          .labelName(key)
          .fillColor(instance.getFillColor())
          .infos(infos())
          .measurements(instance.getMeasurements())
          .polygon(instance.getPolygon());
    }

    public List<ExportAreaPictureAnnotationInstanceInfo> infos() {
      return this.instances.stream()
          .map(ExportAreaPictureAnnotationInstance::getInfos)
          .map(infos -> infos.stream().filter(info -> !KEY_LABEL.equals(info.getLabel())).toList())
          .max(Comparator.comparing(List::size))
          .orElse(List.of());
    }

    public static List<GroupedByKey> from(List<ExportAreaPictureAnnotationInstance> instances) {
      Map<String, List<ExportAreaPictureAnnotationInstance>> grouped = new LinkedHashMap<>();

      for (var instance : instances) {
        grouped.computeIfAbsent(getKey(instance), k -> new ArrayList<>()).add(instance);
      }

      List<GroupedByKey> result = new ArrayList<>();
      for (var entry : grouped.entrySet()) {
        result.add(new GroupedByKey(entry.getKey(), entry.getValue()));
      }

      return result;
    }
  }

  public static String getKey(ExportAreaPictureAnnotationInstance instance) {
    var key =
        instance.getInfos().stream().filter(info -> KEY_LABEL.equals(info.getLabel())).findFirst();

    return key.map(ExportAreaPictureAnnotationInstanceInfo::getValue)
        .orElse(randomUUID().toString());
  }

  private static String base64ToUri(String base64Image) {
    return !base64Image.startsWith("data:") ? BASE_64_URI_PREFIX + base64Image : base64Image;
  }

  private EmojiReplacer getEmojiReplacer() {
    try {
      return new EmojiReplacer(
          new ClassPathResource(EMOJI_PATH).getFile().toPath(),
          "<span class=\"emoji\">",
          "</span>");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
