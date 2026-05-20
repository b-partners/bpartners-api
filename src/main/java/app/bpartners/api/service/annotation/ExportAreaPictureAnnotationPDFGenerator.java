package app.bpartners.api.service.annotation;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactory.createContext;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.User;
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

@Component
public class ExportAreaPictureAnnotationPDFGenerator {
  private final TemplateResolverEngine templateResolverEngine;
  private final EmojiReplacer emojiReplacer;
  private final BucketComponent bucketComponent;

  public static final String KEY_LABEL = "key";
  public static final String FONT_NAME = "Kumbh Sans";
  private static final String EMOJI_PATH = "fonts/twemoji/v/14.0.2/svg";
  private static final String FONT_PATH = "fonts/KumbhSans-Regular.ttf";
  private static final String AREA_PICTURE_ANNOTATION_TEMPLATE = "export-area-picture-annotations";

  public ExportAreaPictureAnnotationPDFGenerator(
      TemplateResolverEngine templateResolverEngine, BucketComponent bucketComponent) {
    this.templateResolverEngine = templateResolverEngine;
    this.emojiReplacer = getEmojiReplacer();
    this.bucketComponent = bucketComponent;
  }

  @SneakyThrows
  public byte[] apply(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages) {

    var html =
        parseDataToString(user, logoBase64, annotation, annotationImages, annotation3DImages);
    if (annotation.getLlm() != null) {
      html = emojiReplacer.replaceEmoji(html);
    }

    try (var outputStream = new ByteArrayOutputStream()) {
      var builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html, null);
      builder.useSVGDrawer(new BatikSVGDrawer());
      builder.useFont(
          () -> {
            try {
              return new ClassPathResource(FONT_PATH).getInputStream();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          },
          FONT_NAME);

      builder.toStream(outputStream);
      builder.run();

      return outputStream.toByteArray();
    } catch (RuntimeException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private String parseDataToString(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages) {
    var templateEngine = templateResolverEngine.getTemplateEngine();

    var context =
        createContext(
            user, logoBase64, annotation, annotationImages, annotation3DImages, bucketComponent);
    return templateEngine.process(AREA_PICTURE_ANNOTATION_TEMPLATE, context);
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
        String key = getKey(instance);
        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
      }

      List<GroupedByKey> result = new ArrayList<>();
      for (var entry : grouped.entrySet()) {
        result.add(new GroupedByKey(entry.getKey(), entry.getValue()));
      }

      return result;
    }
  }

  public static String getKey(ExportAreaPictureAnnotationInstance instance) {
    return instance.getInfos().stream()
        .filter(info -> KEY_LABEL.equals(info.getLabel()))
        .map(ExportAreaPictureAnnotationInstanceInfo::getValue)
        .findFirst()
        .orElse("generated_key_" + randomUUID());
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
