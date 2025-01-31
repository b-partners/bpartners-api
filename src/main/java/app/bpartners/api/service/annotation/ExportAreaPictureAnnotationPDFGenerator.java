package app.bpartners.api.service.annotation;

import static app.bpartners.api.file.FileWriter.base64Image;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationPDFGenerator {
  private final TemplateResolverEngine templateResolverEngine;
  private final FileWriter fileWriter;
  private static final String AREA_PICTURE_ANNOTATION_TEMPLATE = "export-area-picture-annotations";

  public File apply(byte[] annotationImage, ExportAreaPictureAnnotation annotation) {
    var renderer = new ITextRenderer();
    renderer.setDocumentFromString(parseDataToString(annotationImage, annotation));
    renderer.layout();

    var outputStream = new ByteArrayOutputStream();
    try {
      renderer.createPDF(outputStream);
    } catch (DocumentException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return fileWriter.apply(outputStream.toByteArray(), null);
  }

  private String parseDataToString(byte[] annotationImage, ExportAreaPictureAnnotation annotation) {
    TemplateEngine templateEngine = templateResolverEngine.getTemplateEngine();
    Context context = configureContext(annotationImage, annotation);
    return templateEngine.process(AREA_PICTURE_ANNOTATION_TEMPLATE, context);
  }

  private Context configureContext(byte[] annotationImage, ExportAreaPictureAnnotation annotation) {
    var context = new Context();
    context.setVariable("address", annotation.getAddress());
    context.setVariable("annotationImage", base64Image(annotationImage));
    context.setVariable("pages", groupByThree(annotation.getAnnotations()));
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
