package app.bpartners.api.service.annotation;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationPDFGenerator {
  private final TemplateResolverEngine templateResolverEngine;
  private static final String AREA_PICTURE_ANNOTATION_TEMPLATE = "export-area-picture-annotations";
  public static final String KEY_LABEL = "key";

  @SneakyThrows
  public byte[] apply(
      String base64MainImage,
      List<String> base64SubImages,
      ExportAreaPictureAnnotation annotation) {

    String html = parseDataToString(base64MainImage, base64SubImages, annotation);

    EmojReplacer replacer =
        new EmojReplacer(
            new ClassPathResource("fonts/twemoji/v/14.0.2/svg").getFile().toPath(),
            "<span class=\"emoj\">",
            "</span>");
    html = replacer.replaceEmoji(html);

    try (var outputStream = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html, null);
      builder.useSVGDrawer(new BatikSVGDrawer());
      builder.toStream(outputStream);
      loadCustomFonts(builder);
      builder.run();

      return outputStream.toByteArray();
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private String parseDataToString(
      String base64MainImage,
      List<String> base64SubImages,
      ExportAreaPictureAnnotation annotation) {
    TemplateEngine templateEngine = templateResolverEngine.getTemplateEngine();
    Context context = configureContext(base64MainImage, base64SubImages, annotation);
    return templateEngine.process(AREA_PICTURE_ANNOTATION_TEMPLATE, context);
  }

  private Context configureContext(
      String base64MainImage,
      List<String> base64SubImages,
      ExportAreaPictureAnnotation annotation) {
    var context = new Context();

    String mainDataUri =
        base64MainImage != null && !base64MainImage.startsWith("data:")
            ? "data:image/png;base64," + base64MainImage
            : base64MainImage;

    List<String> subDataUris =
        base64SubImages == null
            ? Collections.emptyList()
            : base64SubImages.stream()
                .map(
                    b64 ->
                        b64 != null && !b64.startsWith("data:")
                            ? "data:image/png;base64," + b64
                            : b64)
                .toList();

    context.setVariable("subImages", subDataUris);
    context.setVariable("mainImage", mainDataUri);
    context.setVariable("llm", annotation.getLlm());
    context.setVariable("address", annotation.getAddress());
    context.setVariable("pages", groupByThree(GroupedByKey.from(annotation.getAnnotations())));
    context.setVariable("globalRateType", annotation.getGlobalRateType());
    context.setVariable("globalRateValue", annotation.getGlobalRateValue());
    context.setVariable(
        "degradationLevels",
        Arrays.asList(
            Map.of("label", "A", "color", "#47BE62"),
            Map.of("label", "B", "color", "#F4FBAB"),
            Map.of("label", "C", "color", "#F9DD56"),
            Map.of("label", "D", "color", "#F38F4B"),
            Map.of("label", "E", "color", "#EF2C2D")));

    return context;
  }

  public static List<List<GroupedByKey>> groupByThree(List<GroupedByKey> list) {
    List<List<GroupedByKey>> pages = new ArrayList<>();

    var iterator = list.iterator();
    while (iterator.hasNext()) {
      List<GroupedByKey> page = new ArrayList<>();
      for (int i = 0; i < 3 && iterator.hasNext(); i++) page.add(iterator.next());
      pages.add(page);
    }

    return pages;
  }

  private void loadCustomFonts(PdfRendererBuilder builder) {
    try {
      builder.useFont(
          new ClassPathResource("fonts/KumbhSans-VariableFont_YOPQ,wght.ttf").getFile(),
          "Kumbh Sans");
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public record GroupedByKey(String key, List<ExportAreaPictureAnnotationInstance> instances) {
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
}
