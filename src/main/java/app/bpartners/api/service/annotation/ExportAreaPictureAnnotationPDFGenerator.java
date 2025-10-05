package app.bpartners.api.service.annotation;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationPDFGenerator {
  private final TemplateResolverEngine templateResolverEngine;
  private static final String AREA_PICTURE_ANNOTATION_TEMPLATE = "export-area-picture-annotations";

  public byte[] apply(
      String base64MainImage,
      List<String> base64SubImages,
      ExportAreaPictureAnnotation annotation) {
    var renderer = new ITextRenderer();
    renderer.setDocumentFromString(parseDataToString(base64MainImage, base64SubImages, annotation));
    renderer.layout();

    var outputStream = new ByteArrayOutputStream();
    try {
      renderer.createPDF(outputStream);
    } catch (DocumentException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return outputStream.toByteArray();
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

    context.setVariable("subImages", base64SubImages);
    context.setVariable("llm", annotation.getLlm());
    context.setVariable("mainImage", base64MainImage);
    context.setVariable("address", annotation.getAddress());
    context.setVariable("pages", groupByThree(annotation.getAnnotations()));
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

  public static List<List<ExportAreaPictureAnnotationInstance>> groupByThree(
      List<ExportAreaPictureAnnotationInstance> list) {
    var pages = new ArrayList<List<ExportAreaPictureAnnotationInstance>>();
    var iterator = list.iterator();

    while (iterator.hasNext()) {
      var page = new ArrayList<ExportAreaPictureAnnotationInstance>();
      for (int i = 0; i < 3 && iterator.hasNext(); i++) {
        page.add(iterator.next());
      }
      pages.add(page);
    }
    return pages;
  }
}
